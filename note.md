# print stack
Exception e = new Exception("this is a log");
e.printStackTrace();


# execution stack
```java
StarterSDB.java
    QueryExecutionBase.java(src/main/java/org/apache/jena/sparql/engine/QueryExecutionBase.java)
    :198 public ResultSet execSelect()
        :595 private ResultSet execResultSet()
            :539 private void startQueryIterator()
                :524 protected void execInit()
                :553 queryIterator = getPlan().iterator();
                    :600 public Plan getPlan()
                        QueryEngineSDB.java(src/main/java/org/apache/jena/sdb/engine/QueryEngineSDB.java)
                        :179 public Plan create(Query query, DatasetGraph dataset, Binding inputBinding, Context context)
                            :61 public QueryEngineSDB(DatasetGraphSDB dsg, Query query, Binding initialBinding, Context context)
                                :77 private void init(DatasetGraphSDB dsg, Query query, Binding initialBinding, Context context)
                                    SDBCompile.java (src/main/java/org/apache/jena/sdb/compiler/SDBCompile.java)
                                    :52 public static Op compile(Store store, Op op, Binding binding, Context context, SDBRequest request)
                            QueryEngineBase.java(src/main/java/org/apache/jena/sparql/engine/QueryEngineBase.java)
                            :110 public Plan getPlan()
                                :118 protected Plan createPlan()   
                                    :165 final public QueryIterator evaluate(Op op, DatasetGraph dsg, Binding binding, Context context)
                                        QueryEngineSDB.java(src/main/java/org/apache/jena/sdb/engine/QueryEngineSDB.java)
                                        :126 public QueryIterator eval(Op op, DatasetGraph dsg, Binding binding, Context context)
                                            OpSQL.java (src/main/java/org/apache/jena/sdb/compiler/OpSQL.java)
                                            :60 public QueryIterator exec(Binding parent, ExecutionContext execCxt)             
                                                SDB_QC.java(src/main/java/org/apache/jena/sdb/compiler/SDB_QC.java)
                                                :51 public static QueryIterator exec(OpSQL opSQL, SDBRequest request, Binding binding, ExecutionContext execCxt)
```

```java
StarterTDB.java
    QueryExecutionBase.java(src/main/java/org/apache/jena/sparql/engine/QueryExecutionBase.java)
    :198 public ResultSet execSelect()
        :595 private ResultSet execResultSet()
            :539 private void startQueryIterator()
                :524 protected void execInit()
                :553 queryIterator = getPlan().iterator();
                    :600 public Plan getPlan()
                        QueryEngineTDB.java(src/main/java/org/apache/jena/tdb/solver/QueryEngineTDB.java)
                        :133 public Plan create(Query query, DatasetGraph dataset, Binding inputBinding, Context context)
                            :62 protected QueryEngineTDB(Query query, DatasetGraphTDB dataset, Binding input, Context cxt)
                            QueryEngineBase.java(src/main/java/org/apache/jena/sparql/engine/QueryEngineBase.java)
                            :112 public Plan getPlan()
                                :118 protected Plan createPlan()   
                                    :165 final public QueryIterator evaluate(Op op, DatasetGraph dsg, Binding binding, Context context)
                                        QueryEngineTDB.java(src/main/java/org/apache/jena/tdb/solver/QueryEngineTDB.java)
                                        :97 public QueryIterator eval(Op op, DatasetGraph dsg, Binding binding, Context context)
                                            QueryEngineMain.java(src/main/java/org/apache/jena/sparql/engine/main/QueryEngineMain.java)
                                            56: public QueryIterator eval(Op op, DatasetGraph dsg, Binding input, Context context)
                                                QC.java(src/main/java/org/apache/jena/sparql/engine/main/QC.java)
                                                :45 public static QueryIterator execute(Op op, QueryIterator qIter, ExecutionContext execCxt)
                                                    OpExecutor.java(src/main/java/org/apache/jena/sparql/engine/main/OpExecutor.java)
                                                    :87 static QueryIterator execute(Op op, QueryIterator qIter, ExecutionContext execCxt)
                                                        OpExecutorTDB1.java (src/main/java/org/apache/jena/tdb/solver/OpExecutorTDB1.java)
                                                        :83 protected QueryIterator exec(Op op, QueryIterator input)
                                                            OpExecutor.java(src/main/java/org/apache/jena/sparql/engine/main/OpExecutor.java)
                                                            :113 protected QueryIterator exec(Op op, QueryIterator input)
                                                                ExecutionDispatch.java(src/main/java/org/apache/jena/sparql/engine/main/ExecutionDispatch.java)
                                                                :42 QueryIterator exec(Op op, QueryIterator input)
                                                                    :236 public void visit(OpProject opProject)
                                                                        OpExecutor.java(src/main/java/org/apache/jena/sparql/engine/main/OpExecutor.java)
                                                                        381: protected QueryIterator execute(OpProject opProject, QueryIterator input)
                                                                            :61 public void visit(OpQuadPattern quadPattern)
                                                                                :160 protected QueryIterator execute(OpQuadPattern quadPattern, QueryIterator input)
                                                                                    OpExecutorTDB1.java(src/main/java/org/apache/jena/tdb/solver/OpExecutorTDB1.java)
                                                                                    :130 protected QueryIterator execute(OpQuadPattern quadPattern, QueryIterator input)
                                                                                        :201 private static QueryIterator optimizeExecuteQuads(DatasetGraphTDB ds, QueryIterator input, Node gn, BasicPattern bgp, ExprList exprs, ExecutionContext execCxt)
                                                                                            :173 private static QueryIterator optimizeExecuteTriples(DatasetGraphTDB dsgtdb, QueryIterator input,   BasicPattern pattern, ExprList exprs, ExecutionContext execCxt)
                                                                                                :252 private static BasicPattern reorder(BasicPattern pattern, QueryIterPeek peek, ReorderTransformation transform)
            


```
```java
java.lang.Exception: this is a log
        at org.apache.jena.sparql.engine.main.QC.execute(QC.java:47)
        at org.apache.jena.tdb.solver.OpExecutorTDB1.plainExecute(OpExecutorTDB1.java:258)
        at org.apache.jena.tdb.solver.OpExecutorTDB1.optimizeExecuteTriples(OpExecutorTDB1.java:203)
        at org.apache.jena.tdb.solver.OpExecutorTDB1.optimizeExecuteQuads(OpExecutorTDB1.java:219)
        at org.apache.jena.tdb.solver.OpExecutorTDB1.execute(OpExecutorTDB1.java:148)
        at org.apache.jena.sparql.engine.main.ExecutionDispatch.visit(ExecutionDispatch.java:66)
        at org.apache.jena.sparql.algebra.op.OpQuadPattern.visit(OpQuadPattern.java:92)
        at org.apache.jena.sparql.engine.main.ExecutionDispatch.exec(ExecutionDispatch.java:46)
        at org.apache.jena.sparql.engine.main.OpExecutor.exec(OpExecutor.java:118)
        at org.apache.jena.tdb.solver.OpExecutorTDB1.exec(OpExecutorTDB1.java:87)
        at org.apache.jena.sparql.engine.main.OpExecutor.execute(OpExecutor.java:390)
        at org.apache.jena.sparql.engine.main.ExecutionDispatch.visit(ExecutionDispatch.java:267)
        at org.apache.jena.sparql.algebra.op.OpProject.visit(OpProject.java:47)
        at org.apache.jena.sparql.engine.main.ExecutionDispatch.exec(ExecutionDispatch.java:46)
        at org.apache.jena.sparql.engine.main.OpExecutor.exec(OpExecutor.java:118)
        at org.apache.jena.tdb.solver.OpExecutorTDB1.exec(OpExecutorTDB1.java:87)
        at org.apache.jena.sparql.engine.main.OpExecutor.execute(OpExecutor.java:89)
        at org.apache.jena.sparql.engine.main.QC.execute(QC.java:49)        
(project (?X ?Y ?Z)
  (quadpattern
    (quad <urn:x-arq:DefaultGraphNode> ?X <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#GraduateStudent>)
    (quad <urn:x-arq:DefaultGraphNode> ?Y <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#University>)
    (quad <urn:x-arq:DefaultGraphNode> ?Z <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#Department>)
    (quad <urn:x-arq:DefaultGraphNode> ?X <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#memberOf> ?Z)
    (quad <urn:x-arq:DefaultGraphNode> ?Z <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#subOrganizationOf> ?Y)
    (quad <urn:x-arq:DefaultGraphNode> ?X <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#undergraduateDegreeFrom> ?Y)
  ))
```


SDB algebra expressions
```
(OpSQL --------
  SqlSelectBlock/S_1                     -- V_1=?X V_2=?Y V_3=?Z
                                         -- V_4=?X V_5=?Y V_6=?Z
      R_1.lex/V_1_lex R_1.datatype/V_1_datatype R_1.lang/V_1_lang R_1.type/V_1_type 
      R_2.lex/V_2_lex R_2.datatype/V_2_datatype R_2.lang/V_2_lang R_2.type/V_2_type 
      R_3.lex/V_3_lex R_3.datatype/V_3_datatype R_3.lang/V_3_lang R_3.type/V_3_type 
      R_1.lex/V_4_lex R_1.datatype/V_4_datatype R_1.lang/V_4_lang R_1.type/V_4_type 
      R_2.lex/V_5_lex R_2.datatype/V_5_datatype R_2.lang/V_5_lang R_2.type/V_5_type 
      R_3.lex/V_6_lex R_3.datatype/V_6_datatype R_3.lang/V_6_lang R_3.type/V_6_type
    Join/left outer
      Join/left outer
        Join/left outer
          Join/inner
            Join/inner
              Join/inner
                Join/inner
                  Join/inner
                    Table T_1            -- ?X rdf:type ub:GraduateStudent
                    Table T_2            -- ?Y rdf:type ub:University
                    Condition T_1.p = -6430697865200335348 -- Const: rdf:type
                    Condition T_1.o = 3868028305922703524 -- Const: ub:GraduateStudent
                    Condition T_2.p = -6430697865200335348 -- Const: rdf:type
                    Condition T_2.o = 3234288859141836178 -- Const: ub:University
                  Table T_3              -- ?Z rdf:type ub:Department
                  Condition T_3.p = -6430697865200335348 -- Const: rdf:type
                  Condition T_3.o = 292716264954457753 -- Const: ub:Department
                Table T_4                -- ?X ub:memberOf ?Z
                Condition T_4.p = 2324457024993894247 -- Const: ub:memberOf
                Condition T_1.s = T_4.s  -- Join var: ?X
                Condition T_3.s = T_4.o  -- Join var: ?Z
              Table T_5                  -- ?Z ub:subOrganizationOf ?Y
              Condition T_5.p = -8635217119823486972 -- Const: ub:subOrganizationOf
              Condition T_2.s = T_5.o    -- Join var: ?Y
              Condition T_3.s = T_5.s    -- Join var: ?Z
            Table T_6                    -- ?X ub:undergraduateDegreeFrom ?Y
            Condition T_6.p = 1529584629654714385 -- Const: ub:undergraduateDegreeFrom
            Condition T_1.s = T_6.s      -- Join var: ?X
            Condition T_2.s = T_6.o      -- Join var: ?Y
          Table R_1                      -- Var: ?X
          Condition T_1.s = R_1.hash
        Table R_2                        -- Var: ?Y
        Condition T_2.s = R_2.hash
      Table R_3                          -- Var: ?Z
      Condition T_3.s = R_3.hash
--------)
```
SQL query
```sql
SELECT                                   -- V_1=?X V_2=?Y V_3=?Z
                                         -- V_4=?X V_5=?Y V_6=?Z
  R_1.lex AS V_1_lex, R_1.datatype AS V_1_datatype, R_1.lang AS V_1_lang, R_1.type AS V_1_type, 
  R_2.lex AS V_2_lex, R_2.datatype AS V_2_datatype, R_2.lang AS V_2_lang, R_2.type AS V_2_type, 
  R_3.lex AS V_3_lex, R_3.datatype AS V_3_datatype, R_3.lang AS V_3_lang, R_3.type AS V_3_type, 
  R_1.lex AS V_4_lex, R_1.datatype AS V_4_datatype, R_1.lang AS V_4_lang, R_1.type AS V_4_type, 
  R_2.lex AS V_5_lex, R_2.datatype AS V_5_datatype, R_2.lang AS V_5_lang, R_2.type AS V_5_type, 
  R_3.lex AS V_6_lex, R_3.datatype AS V_6_datatype, R_3.lang AS V_6_lang, R_3.type AS V_6_type
FROM
    Triples AS T_1                       -- ?X rdf:type ub:GraduateStudent
  INNER JOIN
    Triples AS T_2                       -- ?Y rdf:type ub:University
  ON ( T_1.p = -6430697865200335348      -- Const: rdf:type
    AND T_1.o = 3868028305922703524      -- Const: ub:GraduateStudent
    AND T_2.p = -6430697865200335348     -- Const: rdf:type
    AND T_2.o = 3234288859141836178      -- Const: ub:University
   )
  INNER JOIN
    Triples AS T_3                       -- ?Z rdf:type ub:Department
  ON ( T_3.p = -6430697865200335348      -- Const: rdf:type
    AND T_3.o = 292716264954457753       -- Const: ub:Department
   )
  INNER JOIN
    Triples AS T_4                       -- ?X ub:memberOf ?Z
  ON ( T_4.p = 2324457024993894247       -- Const: ub:memberOf
    AND T_1.s = T_4.s                    -- Join var: ?X
    AND T_3.s = T_4.o                    -- Join var: ?Z
   )
  INNER JOIN
    Triples AS T_5                       -- ?Z ub:subOrganizationOf ?Y
  ON ( T_5.p = -8635217119823486972      -- Const: ub:subOrganizationOf
    AND T_2.s = T_5.o                    -- Join var: ?Y
    AND T_3.s = T_5.s                    -- Join var: ?Z
   )
  INNER JOIN
    Triples AS T_6                       -- ?X ub:undergraduateDegreeFrom ?Y
  ON ( T_6.p = 1529584629654714385       -- Const: ub:undergraduateDegreeFrom
    AND T_1.s = T_6.s                    -- Join var: ?X
    AND T_2.s = T_6.o                    -- Join var: ?Y
   )
  LEFT OUTER JOIN
    Nodes AS R_1                         -- Var: ?X
  ON ( T_1.s = R_1.hash )
  LEFT OUTER JOIN
    Nodes AS R_2                         -- Var: ?Y
  ON ( T_2.s = R_2.hash )
  LEFT OUTER JOIN
    Nodes AS R_3                         -- Var: ?Z
  ON ( T_3.s = R_3.hash )
```

src/main/java/org/apache/jena/tdb/solver/OpExecutorTDB1.java
