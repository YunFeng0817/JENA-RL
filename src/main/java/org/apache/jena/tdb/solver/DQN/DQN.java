/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.tdb.solver.DQN;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.iterator.QueryIterPeek;
import org.apache.jena.sparql.engine.iterator.QueryIterRoot;
import org.apache.jena.tdb.solver.OpExecutorTDB1;
import org.apache.jena.tdb.solver.stats.StatsResults;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
import org.deeplearning4j.rl4j.policy.*;
import org.deeplearning4j.rl4j.util.DataManager;
import org.nd4j.linalg.learning.config.Adam;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning;
import org.deeplearning4j.rl4j.space.Box;

public class DQN {

    private int dimension;
    private ArrayList<Integer> Result; // store the final join sequence
    private BasicPattern pattern; // store all original triples
    private Op op;
    private QueryIterator input;
    private ExecutionContext execCxt;
    private final String QFile = "./Q.hashmap"; // the file stored the Q value table
    private StatsResults statsResults;
    private final static String statisticsFile = "Statistics.object";
    private final static String indexEncodingFile = "indexEncoding.object";
    private final static String indexDecodingFile = "indexDecoding.object";

    private static Map<String, Integer> indexEncoding = null;
    private static Map<Integer, String> indexDecoding = null;

    public static QLearning.QLConfiguration CARTPOLE_QL = new QLearning.QLConfiguration(123, // Random seed
            2000, // Max step By epoch
            800, // Max step
            800, // Max size of experience replay
            8, // size of batches
            100, // target update (hard)
            10, // num step noop warmup
            0.1, // reward scaling
            0.99, // gamma
            1.0, // td-error clipping
            0.1f, // min epsilon
            1000, // num step for eps greedy anneal
            false // double DQN
    );

    public static DQNFactoryStdDense.Configuration CARTPOLE_NET = DQNFactoryStdDense.Configuration.builder()
            .updater(new Adam(0.001)).numHiddenNodes(16).numLayer(3).build();

    public DQN(BasicPattern pattern, ExecutionContext execCxt) {
        this.pattern = pattern;
        this.execCxt = execCxt;

        encodeIndexes();

    }

    public void train() throws IOException {
        // record the training data in rl4j-data in a new folder (save)
        DataManager manager = new DataManager();

        // define the mdp from gym (name, render)
        BgpMDP<Box, Integer, BgpActionSpace> mdp = new BgpMDP(this.dimension, pattern, execCxt);
        // define the training
        BgpLearning<Box> dql = new BgpLearning(mdp, CARTPOLE_NET, CARTPOLE_QL, manager);

        // train
        dql.train();

        // get the final policy
        DQNPolicy<Box> pol = dql.getPolicy();

        // serialize and save (serialization showcase, but not required)
        pol.save("./pol1");

        // close the mdp (close http)
        mdp.close();
    }

    public void plan() throws IOException {
        // load the previous agent
        BgpPolicy pol2 = BgpPolicy.load("./pol1");

        BgpMDP<Box, Integer, BgpActionSpace> mdp2 = new BgpMDPPolicy(this.dimension, pattern, execCxt);
        pol2.setMdp(mdp2);

        // evaluate the agent
        double rewards = 0;
        mdp2.reset();
        double reward = pol2.play(mdp2);
        rewards += reward;
        Logger.getAnonymousLogger().info("Reward: " + reward);

        Logger.getAnonymousLogger().info("average: " + rewards / 1000);

    }

    public void encodeIndexes() {
        try {
            indexEncoding = (Map<String, Integer>) readFile(indexEncodingFile);
            indexDecoding = (Map<Integer, String>) readFile(indexDecodingFile);
        } catch (IOException e) {
            /**
             * get statistics data from the object file
             */
            try {
                statsResults = (StatsResults) readFile(statisticsFile);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            indexEncoding = new HashMap<>();
            indexDecoding = new HashMap<>();
            int indexCount = 0;
            for (Node node : statsResults.getPredicates().keySet()) {
                indexEncoding.put(getIndexString(node, "Predicate"), indexCount);
                indexDecoding.put(indexCount, getIndexString(node, "Predicate"));
                indexCount++;
            }
            for (Node node : statsResults.getTypes().keySet()) {
                indexEncoding.put(getIndexString(node, "Type"), indexCount);
                indexDecoding.put(indexCount, getIndexString(node, "Type"));
                indexCount++;
            }
            writeFile(indexEncoding, indexEncodingFile);
            writeFile(indexDecoding, indexDecodingFile);
        }
        this.dimension = indexEncoding.size();
    }

    public static String getIndexString(Node node, String type) {
        switch (type) {
            case "Predicate":
                return "P" + node.getURI();
            case "Type":
                return "T" + node.getURI();
            default:
                return "";
        }
    }

    /**
     * get the index value of one triple string
     * 
     * @param index the triple string
     * @return the index value. if value==-1, the triple isn't in the DB
     */
    public static int encodeIndex(String index) {
        try {
            return indexEncoding.get(index);
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public static String decodeIndex(int code) {
        return indexDecoding.get(code);
    }

    void initInputIterator() {
        this.input = QueryIterRoot.create(execCxt);
        QueryIterPeek peek = QueryIterPeek.create(this.input, execCxt);
        this.input = peek; // Must pass on
    }

    /**
     * run query to get execution time
     * 
     * @return execution time. unit: ms
     */
    long runQuery() {
        initInputIterator();
        List<Triple> triples = pattern.getList();
        BasicPattern newPattern = new BasicPattern();
        Result.forEach(o -> newPattern.add(triples.get(o)));
        op = new OpBGP(newPattern);
        QueryIterator q = OpExecutorTDB1.plainExecute(op, this.input, execCxt);
        long startTime = System.currentTimeMillis();
        for (; q.hasNext(); q.nextBinding())
            ;
        return System.currentTimeMillis() - startTime;
    }

    /**
     * write an object into a file
     * 
     * @param object   the object to write
     * @param fileName file name to write
     */
    public static void writeFile(Object object, String fileName) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
            oos.flush();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * read an object from an file that is written by writeFile function
     * 
     * @param fileName file name to read from
     * @return the Object read from the file
     * @throws IOException
     */
    public static Object readFile(String fileName) throws IOException {
        Object result = null;
        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            result = ois.readObject();
            ois.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }
}