# Replicated (using 2-Phase Commit) Multi-threaded Key-Value Store using RPC

------------------------------------------------------
## Files
------------------------------------------------------
### File - server_config.json

Addresses and ports of coordinator and all participants are in this file. We can bring up them manually and  both the coordinator and participants will read and parse the file to get either participants information or coordinator information.

### File - pom.xml

This file has this maven project configurations and contains all dependencies and build plugin.

### File - _src/main/proto/coordination.proto_

This is the proto file defining the service and all rpc methods and requests/reply messages. Service provided by cooridinator including service for clients and service for participants are defined in this proto file. gRPC client and server interfaces can be generated according to this proto file. (Proto3 compiler is used to compile the proto file).

### File - _src/main/proto/participation.proto_
This is the proto file defining the service and all rpc methods and requests/reply messages. Service provided by participants including service for clients and service for coordinators are defined in this proto file. gRPC client and server interfaces can be generated according to this proto file. (Proto3 compiler is used to compile the proto file).

### File - _src/main/resources/log4j2.xml_
this xml file contains the configuration of log4j logger, and will be used by server and clients. logging level for all is set to INFO, which can be also changed to other logging level. Note that you may see more messages from gRPC handler if you change the logging level to DEBUG. 

------------------------------------------------------
## Generated sources
------------------------------------------------------

generated sources is located in _target/generated-sources_. Interface and classes can be found in _target/generated-sources/protobuf/grpc-java/com.angrycyz.grpc_ and  _target/generated-sources/protobuf/java/com.angrycyz.grpc_ 


------------------------------------------------------
## Classes
------------------------------------------------------

_src/main/java/com.angrycyz_

### Coordinator

To compile Coordinator.java, go to the _replicated_key_value_server_ (actually it should be .....store instead of server, this is a naming typo happens when created the maven project), use:

    mvn compile
    
All the java files under  _replicated_key_value_server/src/main/java/com/angrycyz_ are to be compiled, and the proto files are also to be compiled. Please check the section Generated sources for the location of generated java file.

To execute the coordinator, use:

    mvn exec:java -Dexec.mainClass="com.angrycyz.Coordinator"

The coordiator will firstly read and parse the server_config.json file, config itself using the port and participants information from the file. Then it start service through the port where both client and participant could access. There are two services, one provided for clients to do operation on the key-value map, another one provided for participants to ask for confirmation or decision.

When client put new entry or delete some entry from the map, the coordinator will interact with the participants to conduct two-phase commit. It firstly send a commit query to all participants and collect the votes from participants. If any of the participants voted no it will then send a rollback message to the participants to ask them abort the previous operation. While when all participants voted yes, the coordinator will send a commit message to ask the participants to complete the operation. When all acknowledgement have been received, it completes the transaction.

The program increase the availability and server bandwidth by allowing multiple clients to connect to different replica to use the service and make sure the copies are always the same.  

If the coordinator found any of the participant was shut down, it remove the participant from its participants list and continue providing the service with less replica number to ensure the . The program does not support bringing the replica again and also does not support synchronizing the replica with the others when restarting.

### Participant

if you haven't compile the files, please compile them with:

    mvn compile

run the participant with a given port number, the 4 participant port and address can be found in server_config.json, which can also be modified as other ports.
    
    mvn exec:java -Dexec.mainClass="com.angrycyz.Participant" -Dexec.args="12112"
    mvn exec:java -Dexec.mainClass="com.angrycyz.Participant" -Dexec.args="12113"
    mvn exec:java -Dexec.mainClass="com.angrycyz.Participant" -Dexec.args="12114"
    mvn exec:java -Dexec.mainClass="com.angrycyz.Participant" -Dexec.args="12115"

### Client
you can use client to connect to any of the servers, simply by configuring the port number in server_config.json and give an address and a port number as the client input:

connect to coordinator:
    
    mvn exec:java -Dexec.mainClass="com.angrycyz.Client" -Dexec.args="localhost 12111"
    
connect to one participant:
    
    mvn exec:java -Dexec.mainClass="com.angrycyz.Client" -Dexec.args="localhost 12112"
    

### Transaction

Transaction contains transaction id, key, value and decision. transaction id is represented by random UUID to make sure it's unique. If particiant asks for decision of some transaction, it can get the decision with given transaction id.

### Utility

This class contains methods and fields which are used by different classes.

### SercerConfig

server_config.json will be parsed and stored in this class.

