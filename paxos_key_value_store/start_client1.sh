echo "Starting client 1 to connect to port 12116";
mvn exec:java -Dexec.mainClass="com.angrycyz.Client" -Dexec.args="localhost 12116" < input1;
