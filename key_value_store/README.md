# Single Server, Key Value Store

------------------------------------------------------
### File - comm_config.json

this json file contains only one field - communication protocol, the value of it should either be **"TCP"** or **"UDP"**

### File - log4j2.xml

this xml file contains the configuration of log4j logger, and will be used by server and clients.

### File - pom.xml

this xml file contains maven configurations and all dependencies

------------------------------------------------------

## Class - KeyValueStoreServer

this is a server which pre-poplates key-value pairs to hashmap(in memory) and read communication protocol configuration to decide to open a TCP server or UDP server.

to run the server, you should firstly complie it, since it's a maven project, so compile it in key_value_store directory using:

    mvn compile

ro run it, give it a port number as argument, for example:

    mvn exec:java -Dexec.mainClass="com.angrycyz.KeyValueStoreServer" -Dexec.args="1254"
    
the mainClass represents for the class to execute, the args define the arguments. If you don't specify the port, the program will ask you for a valid port infinitely. Note all classes are in the package com.angrycyz, so make sure you add the package name before the class name.


------------------------------------------------------
## Interface - Server

this is an interface and declares only one method - __startServer__. The interface is implemented  by TCPServer and UDPServer.

------------------------------------------------------
## TCPServer

this is a server implementing Server interface and create socket to communicate with client using TCP protocal. This is a single server and process the clients requests back to back. Only when one client finishes ot exits, the server responds to another client.

------------------------------------------------------
## UDPServer

this is a server implementing Server interface and directly send datagrams to client by UDP protocol . This is a single server and process the requests back to back. Since no connection is established before sending request, thus the server can process requests from any server.

------------------------------------------------------
## TCPClient

If you have already compiled the projects, ignore this step, otherwise please use:

    mvn compile

to run the client, give it the address and port number, separated by space:

    mvn exec:java -Dexec.mainClass="com.angrycyz.TCPClient" -Dexec.args="127.0.0.1 1254"

If you don't specify the address and port, the program will ask you for an address and a valid port infinitely. 

The client has a 5 seconds timeout for all its requests, if it does not get response from server in 5 seconds, it will try to reconnect to the server to establish the connection again and ask for the next request. You can test this mechanism by restart the server.

------------------------------------------------------
## UDPClient

If you have already compiled the projects, ignore this step, otherwise please use:

    mvn compile

to run the client, give it the address and port number, separated by space:

    mvn exec:java -Dexec.mainClass="com.angrycyz.TCPClient" -Dexec.args="127.0.0.1 1254"

If you don't specify the address and port, the program will ask you for an address and a valid port infinitely.

The client has a 5 seconds timeout for all its requests, if it does not get response from server in 5 seconds, it will ask for the next request and send the next request. You can test this mechanism by restart the server.

------------------------------------------------------
## Utility

this is a class defines functions which are used by clients and servers.

