echo "Starting client 1 to connect to port 12118";
mvn exec:java -Dexec.mainClass="com.angrycyz.Client" -Dexec.args="localhost 12118" < input3;