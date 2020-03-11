mvn -Drat.skip=true compile
mvn exec:java -Dexec.mainClass="Starter"
# mvn exec:java -Dexec.mainClass="com.vineetmanohar.module.Main" -Dexec.args="arg0 arg1 arg2"
# cd /home/mark/program/GraduateProject/jena-sdb ; /usr/lib/jvm/java-8-openjdk-amd64/bin/java -Dfile.encoding=UTF-8 -cp /tmp/cp_3vz5ep8ur8spmthcvw6fkifxm.jar Starter