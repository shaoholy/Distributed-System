echo "Starting client 1 to connect to port 12117";
mvn exec:java -Dexec.mainClass="com.angrycyz.Client" -Dexec.args="localhost 12117" < input2;
