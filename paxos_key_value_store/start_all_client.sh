sh start_client1.sh & sh start_client2.sh;
mvn exec:java -Dexec.mainClass="com.angrycyz.Client" -Dexec.args="localhost 12116" < input_get
