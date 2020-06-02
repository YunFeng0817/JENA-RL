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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

public class QLearning {

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
    private List<String> triples;
    private FileWriter rewardRecorder = null;
    final ExecutorService exec = Executors.newFixedThreadPool(1);
    Callable<Long> call = new Callable<Long>() {
        public Long call() throws Exception {
            return -runQuery();
        }
    };

    QLearning(BasicPattern pattern, ExecutionContext execCxt) {
        this.columnLength = pattern.size();
        this.pattern = pattern;
        this.execCxt = execCxt;
        this.State = new ArrayList<>();
        this.Q = new HashMap<>();
        init();
        try {
            this.Q = (Map<List<String>, Map<String, Double>>) readFile(this.QFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /**
         * get statistics data from the object file
         */
        try {
            statsResults = (StatsResults) readFile(statisticsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Stats.write(System.out, statisticsResult);
        preProcessingTriples();

        String fileName = "reward.txt";
        try {
            rewardRecorder = new FileWriter(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        for (int i = 0; i < 200; i++) { // Train cycles
            System.out.println("Round count: " + i);
            init();
            while (!isFinalState()) {
                initInputIterator();
                List<Integer> actionsFromCurrentState = possibleActionsFromState();
                Map<String, Double> QFromCurrentState = getQFromCurrentState();

                // Pick a random action from the ones possible
                int index = rand.nextInt(actionsFromCurrentState.size());
                double greedyRate = 0.9;
                double randomValue = rand.nextDouble();
                int choice = 0;
                if (randomValue < greedyRate) {
                    choice = getPolicyFromState();
                } else {
                    choice = actionsFromCurrentState.get(index);
                }
                State.add(triples.get(choice));
                Result.add(choice);

                // Q(state,action)= Q(state,action) + alpha * (R(state,action) + gamma *
                // Max(next state, all actions) - Q(state,action))
                double maxQ = maxQ();
                long maxTime = 1000 * 9;
                double r = -maxTime;

                try {
                    Future<Long> future = exec.submit(call);
                    r = future.get(maxTime, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ex) {
                    System.out.println("Query execution time out!!!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (isFinalState()) {
                    try {
                        rewardRecorder.write((-r) + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                double value = alpha * (r + gamma * maxQ);
                QFromCurrentState.put(triples.get(choice), value);
                System.out.println("State: " + Result.toString());
            }
        }
        writeFile(this.Q, this.QFile);
        exec.shutdown();
        try {
            rewardRecorder.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        // printQ();
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