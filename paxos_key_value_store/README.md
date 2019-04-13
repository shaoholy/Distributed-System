# Paxos-Based, Multi-threaded, Replicated Key-Value Store using RPC

------------------------------------------------------
## Paxos Implementation
------------------------------------------------------
Please see the following diagram: 
![alt text](https://github.com/angrycyz/Distributed-System/blob/master/paxos_key_value_store/diagram.png?raw=true)

The diagram shows how the paxos-based key-value store server is implemented. We have 5 servers, each plays 3 roles: proposer, acceptor and learner. The server starts a new thread for each role, and the master thread works as the coordinator. 

The client could connect to any of the server, send sends request to the proposer

**(Phase 1)**
then the proposer will then send prepare message to all acceptors. In the program, the communication between the proposer and remote acceptor(let's say "remote" for different processes since the address could be configured differently) is based on gRPC, and the communication between the proposer and local acceptor is based on memory sharing, here I use BlockingQueue with timeout on blocking to realize the threads communication.  

when the acceptor receives the prepare request, it will check if this is the first time getting prepare request, if it is, the acceptor promise with the incoming prepare request,  if not, the acceptor then compare the proposal id and server id of the incoming request with the recorded one, it always reply the proposer with the larger one and update the stored request with the larger one.

After the proposer receives the reply from the acceptor in the first phase, it knows if the acceptor give it a promise. If any of the acceptor replys with higher proposal id, the proposer abandon its initial request and use a new proposal id with the acceptor's replying message. If there's no reply with higher offer, the proposer check if it has received the majority reply, if yes, it continues the second phase, otherwise it retries asking for promise. The retry times I set in the program is 3. 

**(Phase 2)**
In the second phase, the proposer sends the proposal to the acceptor, if the acceptor still replies with higher offer, then the proposer will abandon the proposal. Same as the first phase, the proposer also checks if it received the majority reponse from acceptor, if no, it abandon the proposal, and if yes, it announce to all learners according to "Paxos Made Simple".

The learner then respond to the coordinator so that the coordinator sends the result to the clients.


**What happens if any of the server crashes?**

When all servers are up and running, every final proposal made by one proposer will be broadcast to all servers, so as long as the servers never go down, they should always have the same value. But what if any server crashes? The solution is to add one learner API, when the server crashes and restarts, it will call the learner API to get the most recent state and update the map so that to keep the same data as in other servers. As the server goes down, the node number decreases, and the program can still work as long as the server number is larger or equal to 3(to get the majority), when the server restarted, it still can work as a member.


**How does the proposer know if a server is up or down?**

In every phase, the proposer will send request to the remote acceptor, if the acceptor is down, the proposer will get an **io.grpc.StatusRuntimeException**, in this case, we'll decrease the total up server number by 1 and consider the majority according to the updated number. To avoid repeatly decrease the server number, a hashmap for down server address and port  is used. And for every request, the proposer will send request to all servers indicated in the configuration file to check if they are online.


![alt text](https://github.com/angrycyz/Distributed-System/blob/master/paxos_key_value_store/proposerToAcceptor.png?raw=true)

------------------------------------------------------
## Files
------------------------------------------------------
### _server_config.json_

Addresses and ports of all servers are in this file. We can bring up them manually and all of them will parse the file to get others address and port so that to communicate with each other.
If you change the port number, please also use the port you just changed to bring up the servers.

### _src/main/proto/paxosMsg.proto_
This proto file defines a message type which has field proposal id, server id, message and action. Server id is used when there is a tie between the acceptor recorded proposal id and an incoming proposal id. Action could be "promise", "prepare", "propose", "accept", there is no action which called "reject" because the proposer has alrealy contain the logic when acceptor sends promise or accept message with higher proposal id, which may result in "reject".

### _src/main/proto/kvStore.proto_
This proto file defines APIs to operate on the map, which could be get, delete ot put.

### _src/main/proto/acceptPb_
This proto file defines APIs belongs to both acceptor and learner. For acceptor there are _prepare_, _accept_ which can be called by proposer, and for learner there are _getState_, _learnProposal_ which can be called by either server(coordinator) or proposer.

### _log4j2.xml_
this xml file contains the configuration of log4j logger, and will be used by server and clients. logging level for all is set to INFO, which can be also changed to other logging level. Note that you may see more messages from gRPC handler if you change the logging level to DEBUG. 

------------------------------------------------------
## Generated sources
------------------------------------------------------

generated sources is located in _target/generated-sources_. Interface and classes can be found in _target/generated-sources/protobuf/grpc-java/com.angrycyz.grpc_ and  _target/generated-sources/protobuf/java/com.angrycyz.grpc_ 

------------------------------------------------------
## How to run the server and client
------------------------------------------------------

Firstly, compile all classes:

    mvn compile

to execute the 5 servers, use:

    mvn exec:java -Dexec.mainClass="com.angrycyz.Server" -Dexec.args="12116"
    mvn exec:java -Dexec.mainClass="com.angrycyz.Server" -Dexec.args="12117"
    mvn exec:java -Dexec.mainClass="com.angrycyz.Server" -Dexec.args="12118"
    mvn exec:java -Dexec.mainClass="com.angrycyz.Server" -Dexec.args="12119"
    mvn exec:java -Dexec.mainClass="com.angrycyz.Server" -Dexec.args="12110"
    
the argument is the port numbers of the servers, if you would like to use other ports, please also change the configuration file _server_config.json_ to make sure all servers know each other from the configuration file.

to run the client, use:

    mvn exec:java -Dexec.mainClass="com.angrycyz.Client" -Dexec.args="localhost 12117"
    
you can use the client to connect to any server and send "put", "get" or "delete" request. I also add a "clearall" function to simplify the test process.

if you would like to see how the online server number is determined by the proposer, simply grep the keyword "Quorum" from the log in the console, for example:

    mvn exec:java -Dexec.mainClass="com.angrycyz.Server" -Dexec.args="12116" | grep Quorum
    


