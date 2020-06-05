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

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.solver.QLearning;
import org.apache.jena.tdb.solver.stats.Stats;
import org.apache.jena.tdb.solver.stats.StatsCollector;
import org.apache.jena.tdb.solver.stats.StatsResults;

/**
 * Example of the usual way to connect store and issue a query. A description of
 * the connection and store is read from file "sdb.ttl". Use and password come
 * from environment variables SDB_USER and SDB_PASSWORD.
 */

public class StarterTDB {

    final static String statisticsFile = "Statistics.object";
    public static StatsResults statisticsResult = null;
    static public Dataset ds;
    static ExecutorService exec = Executors.newFixedThreadPool(1);

    static QueryCallable call = new QueryCallable();
    static FileWriter rewardRecorder = null;

    static public void main(String... argv) throws IOException {

        Set<String> artifactoryLoggers = new HashSet<>(
                Arrays.asList("org.apache.http", "groovyx.net.http", "org.apache.jena"));
        for (String log : artifactoryLoggers) {
            ch.qos.logback.classic.Logger artLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                    .getLogger(log);
            artLogger.setLevel(ch.qos.logback.classic.Level.INFO);
            artLogger.setAdditive(false);
        }

        String directory = "TDBLUBM";
        ds = TDBFactory.createDataset(directory);
        // used to load data
        Model model = ds.getDefaultModel();
        // create Q Learning BGP optimizer
        QLearning QLearning = new QLearning();
        // send the Q Learning object to the BGP optimizer
        ds.getContext().set(Symbol.create("QLearning"), QLearning);
        // String Optimizer = "Default";
        String Optimizer = "QLearning";
        // String Optimizer = "DQN";
        ds.getContext().set(Symbol.create("Optimizer"), Optimizer);

        String fileName = "reward.txt";
        try {
            rewardRecorder = new FileWriter(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * load part of LUBM data into TDB
         */
        // loadData(model, "./Data/LUBM/", "TURTLE");
        // loadData(model, "./Data/LUBM/", "N-TRIPLE");

        // Query query = loadQuery("./Query/LUBM/2.sparql");
        List<Query> trainQueryList = loadQuerySet("./Query/LUBM/train");
        List<Query> testQueryList = loadQuerySet("./Query/LUBM/test");

        // singleRun(QLearning, query);
        // runQuerySet(QLearning, trainQueryList);
        runQuerySet(QLearning, testQueryList);
        // RLTrain(QLearning, trainQueryList, 200);
        exec.shutdown();
        rewardRecorder.close();
        System.exit(0);
    }

    static void singleRun(QLearning QLearning, Query query) {
        long maxTime = 1000 * 60;
        double r = -maxTime;
        call.setQuery(query);
        try {
            if (exec.isShutdown()) {
                exec = Executors.newFixedThreadPool(1);
            }
            Future<Long> future = exec.submit(call);
            r = -future.get(maxTime, TimeUnit.MILLISECONDS);
            System.out.println("  Time Cost: " + (-r) + "\n");
        } catch (TimeoutException ex) {
            System.out.println("  Query execution time out!!!\n");
            // shut down the tread pool to stop the task
            exec.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            exec.shutdown();
        }
        try {
            rewardRecorder.write((-r) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        QLearning.updateQ(r);
        QLearning.saveQValue();
    }

    static void runQuerySet(QLearning QLearning, List<Query> queryList) {
        for (int i = 0; i < queryList.size(); i++) {
            System.out.println("  Query: " + (i + 1));
            singleRun(QLearning, queryList.get(i));
        }
    }

    /**
     * train the RL model for many episodes all at once
     * 
     * @param QLearning  Q learning object
     * @param query      the query
     * @param trainRound the round the model need to train
     */
    static void RLTrain(QLearning QLearning, List<Query> queryList, int trainRound) {
        for (int i = 0; i < trainRound; i++) {
            System.out.println("Round: " + (i + 1));
            runQuerySet(QLearning, queryList);
        }
    }

    /**
     * execute query, get query results and get time cost
     * 
     * @return time cost, unit: ms
     */
    static long runQuery(Query query) {
        long startTime = System.currentTimeMillis();
        try (QueryExecution qe = QueryExecutionFactory.create(query, ds)) {
            ResultSet rs = qe.execSelect();
            for (; rs.hasNext();) {
                if (Thread.currentThread().isInterrupted())
                    return 0;
                QuerySolution soln = rs.nextSolution();
                // Iterator<String> vars = soln.varNames();
                // for (; vars.hasNext();) {
                // String varName = vars.next();
                // System.out.println(varName + ": " + soln.get(varName).toString());
                // }
                // System.out.println("------------");
            }
        }
        return System.currentTimeMillis() - startTime;
    }

    /**
     * add data to TDB and store statistics data into a file
     * 
     * @param model    graph model object
     * @param filePath data files to be loaded. It could be a file or a folder
     * @param type     data file type: "TURTLE" or "N-TRIPLE" or "N3"
     */
    static void loadData(Model model, String filePath, String type) {
        // load data from files
        System.out.println("Starting loading data...");
        File dataFile = new File(filePath);
        File[] fileList = dataFile.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                System.out.println("Loading data file: " + file.getPath());
                model = model.read(file.getPath(), type);
            }
        } else if (filePath != null) {
            System.out.println("Loading data file: " + filePath);
            model = model.read(filePath, type);
        }
        System.out.println("Finished loading data!");
        StatsCollector sc = Stats.gather(model.getGraph());
        QLearning.writeFile(sc.results(), statisticsFile);
        System.out.println("Finished writing statistics data into file: " + statisticsFile);
    }

    /**
     * load query object from file
     * 
     * @param queryFilePath path of sparql query file
     * @return query object
     * @throws IOException
     */
    static Query loadQuery(String queryFilePath) throws IOException {
        StringBuilder queryString = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(queryFilePath)));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            queryString.append(line);
            queryString.append("\n");
        }
        bufferedReader.close();
        // System.out.println(queryString.toString());
        return QueryFactory.create(queryString.toString());
    }

    /**
     * load query string from file
     * 
     * @param queryDirPath path of sparql query file or directory
     * @return list of query object
     * @throws IOException
     */
    static List<Query> loadQuerySet(String queryDirPath) throws IOException {
        List<Query> queryList = new ArrayList<>();
        File queryDir = new File(queryDirPath);
        File[] fileList = queryDir.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                queryList.add(loadQuery(file.getPath()));
            }
        } else if (queryDirPath != null) {
            queryList.add(loadQuery(queryDirPath));
        }
        return queryList;
    }

    /**
     * implement the callable interface
     */
    static class QueryCallable implements Callable {
        Query Query;

        public void setQuery(Query query) {
            Query = query;
        }

        @Override
        public Long call() throws Exception {
            return runQuery(Query);
        }

    }

}
