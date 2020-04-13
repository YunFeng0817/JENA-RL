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

package org.apache.jena.tdb.solver;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.iterator.QueryIterPeek;
import org.apache.jena.sparql.engine.iterator.QueryIterRoot;
import org.apache.jena.sparql.graph.NodeConst;
import org.apache.jena.tdb.solver.stats.StatsResults;

public class DQN {

    private final double alpha = 0.1; // Learning rate
    private final double gamma = 0.9; // Eagerness - 0 looks in the near future, 1 looks in the distant future

    private int columnLength = 0;

    private Map<List<String>, Map<String, Double>> Q; // Q learning
    private List<String> State; // store state
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
    private List<String> triples;

    private static Map<String, Integer> indexEncoding = null;
    private static Map<Integer, String> indexDecoding = null;

    DQN(BasicPattern pattern, ExecutionContext execCxt) {
        this.columnLength = pattern.size();
        this.pattern = pattern;
        this.execCxt = execCxt;
        this.State = new ArrayList<>();
        this.Q = new HashMap<>();
        encodeIndexes();
        // init();
        // this.Q = (Map<List<String>, Map<String, Double>>) readFile(this.QFile);
        // statsResults = (StatsResults) readFile(statisticsFile);
        // Stats.write(System.out, statisticsResult);
        // preProcessingTriples();
    }

    public void encodeIndexes() {
        try {
            indexEncoding = (Map<String, Integer>) QLearning.readFile(indexEncodingFile);
            indexDecoding = (Map<Integer, String>) QLearning.readFile(indexDecodingFile);
        } catch (IOException e) {
            /**
             * get statistics data from the object file
             */
            try {
                statsResults = (StatsResults) QLearning.readFile(statisticsFile);
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
            QLearning.writeFile(indexEncoding, indexEncodingFile);
            QLearning.writeFile(indexDecoding, indexDecodingFile);
        }
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

    public static int encodeIndex(String index) {
        return indexEncoding.get(index);
    }

    public static String decodeIndex(int code) {
        return indexDecoding.get(code);
    }

    void preProcessingTriples() {
        this.triples = new ArrayList<>();
        for (Triple triple : pattern.getList()) {
            this.triples.add(getTripleIndex(triple));
        }
    }

    String getTripleIndex(Triple triple) {
        if (NodeConst.nodeRDFType.equals(triple.getPredicate())) {
            return triple.getObject().getURI();
        } else if (triple.getPredicate().isConcrete())
            return triple.getPredicate().getURI();
        else {
            Exception e = new Exception("Unknown triple type");
            e.printStackTrace();
            return "";
        }
    }

    void initInputIterator() {
        this.input = QueryIterRoot.create(execCxt);
        QueryIterPeek peek = QueryIterPeek.create(this.input, execCxt);
        this.input = peek; // Must pass on
    }

    /**
     * initiate some variables every time before the start of a new training round
     */
    void init() {
        State.clear();
        Result = new ArrayList<>();
    }

    /**
     * calculate Q value(training process)
     */
    void calculateQ() {
        Random rand = new Random();

        for (int i = 0; i < 20; i++) { // Train cycles
            System.out.println("Round count: " + i);
            init();
            while (!isFinalState()) {
                initInputIterator();
                List<Integer> actionsFromCurrentState = possibleActionsFromState();
                Map<String, Double> QFromCurrentState = getQFromCurrentState();

                // Pick a random action from the ones possible
                int index = rand.nextInt(actionsFromCurrentState.size());
                int choice = actionsFromCurrentState.get(index);
                State.add(triples.get(choice));
                Result.add(choice);

                // Q(state,action)= Q(state,action) + alpha * (R(state,action) + gamma *
                // Max(next state, all actions) - Q(state,action))
                double maxQ = maxQ();

                double r = -runQuery();

                double value = alpha * (r + gamma * maxQ);
                QFromCurrentState.put(triples.get(choice), value);
                System.out.println("State: " + Result.toString());
            }
        }
        writeFile(this.Q, this.QFile);
    }

    /**
     * judge if the current state is final state
     * 
     * @return boolean flag
     */
    boolean isFinalState() {

        return Result.size() == columnLength;
    }

    /**
     * get all possible action at the current state
     * 
     * @return all possible action stored in a list
     */
    List<Integer> possibleActionsFromState() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < columnLength; i++) {
            if (!State.contains(triples.get(i)))
                result.add(i);
        }
        return result;
    }

    /**
     * get action-QValue list at the current state
     *
     * @return the action-QValue list
     */
    Map<String, Double> getQFromCurrentState() {
        if (!Q.containsKey(State)) {
            Map<String, Double> newValue = new HashMap<>();
            Q.put(new ArrayList<>(State), newValue);
        }
        return Q.get(State);

    }

    /**
     * get max Q value at the current state according Q value table
     * 
     * @return
     */
    double maxQ() {
        List<Integer> actionsFromState = possibleActionsFromState();
        Map<String, Double> QFromNextState = getQFromCurrentState();

        // the learning rate and eagerness will keep the W value above the lowest reward
        double maxValue = Double.NEGATIVE_INFINITY;
        for (int nextAction : actionsFromState) {
            if (QFromNextState.containsKey(triples.get(nextAction))) {
                double value = QFromNextState.get(triples.get(nextAction));
                if (value > maxValue)
                    maxValue = value;
            }

        }
        return maxValue == Double.NEGATIVE_INFINITY ? 0 : maxValue;
    }

    /**
     * get the best execution policy according to Q value table and get execution
     * result(time cost)
     */
    void getPolicy() {
        init();
        printQ();
        while (!isFinalState()) {
            int nextAction = getPolicyFromState();
            State.add(triples.get(nextAction));
            Result.add(nextAction);
        }
        long costTime = runQuery();
        System.out.println("Policy: " + Result.toString());
        System.out.println("Final time cost: " + costTime);
    }

    /**
     * get the best action choice at the current state according to Q value table
     * 
     * @return action number(ID)
     */
    int getPolicyFromState() {
        List<Integer> actionsFromState = possibleActionsFromState();
        Map<String, Double> QFromCurrentState = getQFromCurrentState();

        double maxValue = Double.NEGATIVE_INFINITY;
        int policyGotoState = -1;
        // System.out.println(State);
        // System.out.println(QFromCurrentState);

        // Pick to move to the state that has the maximum Q value
        for (int nextAction : actionsFromState) {
            if (QFromCurrentState.containsKey(triples.get(nextAction))) {
                double value = QFromCurrentState.get(triples.get(nextAction));
                if (value > maxValue) {
                    maxValue = value;
                    policyGotoState = nextAction;
                }
            }
        }
        return policyGotoState == -1 ? actionsFromState.get(0) : policyGotoState;
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
     * print Q value
     */
    void printQ() {
        System.out.println("Q matrix");
        for (Map.Entry<List<String>, Map<String, Double>> entry : Q.entrySet()) {
            System.out.println("From state " + entry.getKey().toString() + ": " + entry.getValue().toString());
        }
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
     */
    public static Object readFile(String fileName) {
        Object result = null;
        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            result = ois.readObject();
            ois.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }
}