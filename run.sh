mvn -Drat.skip=true compile
# mvn exec:java -Dexec.mainClass="StarterSDB"
# mvn -q -e exec:java -Dexec.mainClass="StarterTDB"
# mvn -q -e exec:java -Dexec.mainClass="TensorFlowDemo"
mvn -q -e exec:java -Dexec.mainClass="DQN"
# mvn exec:java -Dexec.mainClass="com.vineetmanohar.module.Main" -Dexec.args="arg0 arg1 arg2"