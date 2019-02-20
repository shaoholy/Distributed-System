# Multi-threaded Key-Value Store using RPC

------------------------------------------------------
## Files
------------------------------------------------------
### File - pom.xml

this file has this maven project configurations and contains all dependencies and build plugin.

### File - _src/main/proto/operation.proto_

this is the proto file defining the service and all rpc methods and requests/reply message. gRPC client and server interfaces can be generated according to this proto file. (Note proto3 compiler is used to ompile the proto file)

### File - _src/main/resources/log4j2.xml_
this xml file contains the configuration of log4j logger, and will be used by server and clients. logging level for all is set to INFO, which can be also changed to other logging level. Note that you may see more messages from gRPC handler if you change the logging level to DEBUG. Do not change the logging level to level higher than INFO otherwise you won't see the reply from server in the client console.

------------------------------------------------------
## Generated sources
------------------------------------------------------

generated sources is located in _target/generated-sources_. Interface and classes can be found in _target/generated-sources/protobuf/grpc-java/com.angrycyz.grpc_ and  _target/generated-sources/protobuf/java/com.angrycyz.grpc_ 

------------------------------------------------------
## Classes
------------------------------------------------------

_src/main/java/com.angrycyz_

### KeyValueStoreServer

to compile the files, compile them in _multi_thread_key_value_store/_ direcory, use:
    
    mvn compile

ro run the server, give it a port number as argument, for example:

    mvn exec:java -Dexec.mainClass="com.angrycyz.KeyValueStoreServer" -Dexec.args="1254"

if the port number is not given or not valid, the server will keep asking you for a port number. and if the given port number is not available, the server program will log an error message. 


### KeyValueStoreClient

if you haven't compile the files, please compile them with:

    mvn compile

ro run the client, give it an address and a port number, separated with space, for example:

    mvn exec:java -Dexec.mainClass="com.angrycyz.KeyValueStoreClient" -Dexec.args="localhost 1254"
    
now you can use 

    put <key> <value> 
    get <key>
    delete <key>
    
to make operations on the hashmap, the put/get/delete is not case sensitive. Note the hashmap is pre-populated with 15 key-value pairs and will give a log on the elements in it to make sure you can do operations on it without getting messages like "Key does not exist".
    
### Utility

this is a class defines functions which are used by both client and server.
    


