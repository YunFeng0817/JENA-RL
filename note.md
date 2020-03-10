# print stack
Exception e = new Exception("this is a log");
e.printStackTrace();


# execution stack
starter.java
    QueryExecutionBase.java(src/main/java/org/apache/jena/sparql/engine/QueryExecutionBase.java)
    :198 public ResultSet execSelect()
        :595 private ResultSet execResultSet()
            :539 private void startQueryIterator()
                :524 protected void execInit()
                :553 queryIterator = getPlan().iterator();
                    :600 public Plan getPlan()
                        

