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
import java.util.Iterator;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sdb.SDBFactory;
import org.apache.jena.sdb.Store;
import org.apache.jena.sdb.store.DatasetStore;

/**
 * Example of the usual way to connect store and issue a query. A description of
 * the connection and store is read from file "sdb.ttl". Use and password come
 * from environment variables SDB_USER and SDB_PASSWORD.
 */

public class StarterSDB {
    static public void main(String... argv) throws IOException {

        String queryString = loadQuery("./Query/LUBM/2.sparql");
        Query query = QueryFactory.create(queryString);
        Store store = SDBFactory.connectStore("sdb-pgsql.ttl");
        // Op op = Algebra.compile(query);
        // System.out.println(op.toString());
        // dangerous!!! this line of code will wipe all data in DB
        // store.getTableFormatter().create();

        // load data from files
        // Model model = SDBFactory.connectDefaultModel(store);
        // System.out.println("Starting loading data...");
        // model = model.read("./Data/University2.nt", "N-TRIPLE");
        // System.out.println("Finished loading data!");

        // Must be a DatasetStore to trigger the SDB query engine.
        // Creating a graph from the Store, and adding it to a general
        // purpose dataset will not necessarily exploit full SQL generation.
        // The right answers will be obtained but slowly.

        Dataset ds = DatasetStore.create(store);
        try (QueryExecution qe = QueryExecutionFactory.create(query, ds)) {
            ResultSet rs = qe.execSelect();
            for (; rs.hasNext();) {
                QuerySolution soln = rs.nextSolution();
                Iterator<String> vars = soln.varNames();
                for (; vars.hasNext();) {
                    String varName = vars.next();
                    System.out.println(varName + ": " + soln.get(varName).toString());
                }
                System.out.println("------------");
            }
        }

        // Close the SDB connection which also closes the underlying JDBC connection.
        store.getConnection().close();
        store.close();
    }

    /**
     * load query string from file
     * 
     * @param queryFilePath path of sparql query file
     * @return query string
     * @throws IOException
     */
    static String loadQuery(String queryFilePath) throws IOException {
        StringBuilder queryString = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(queryFilePath)));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            queryString.append(line);
            queryString.append("\n");
        }
        bufferedReader.close();
        // System.out.println(queryString.toString());
        return queryString.toString();
    }

}