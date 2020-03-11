# print stack
Exception e = new Exception("this is a log");
e.printStackTrace();


# execution stack
```java
starter.java
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