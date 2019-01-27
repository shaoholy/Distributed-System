------------------------------------------
## TCPServer
------------------------------------------

Same running steps as the first assignment, compile the java file and run the class file with one argument indicating the port or without any argumen(the program will use a default port).

Example:

    >> javac TCPServer.java
    >> java TCPServer
    or >> java TCPServer 1254

------------------------------------------
## MultiThreadingTCPServer
------------------------------------------

Simply run the class file with one argument indicating the port or with no argument(the program will use a default port).

You can use ctrl+c to shutdown the server, and the server will send a close message to clients. 

When timed out, server will print thread done and socket close message.

Example: 

    >> javac MultiThreadingTCPServer.java
    >> java MultiThreadingTCPServer
    or >> java MultiThreadingTCPServer 1254
    
if you are giving more than one argument, the program will ask you to provide only one agument.

Example:

    >> java MultiThreadingTCPServer 1254 1222
    Please give only one argument
    >> 1254

For each socket the server created, it has a 14 seconds timeout, if you didn't send anything by some socket in 14 seconds, the socket will be closed, and server will send a close message to client to make sure the next time when client tries to send something, the client would know that the corresponding socket is closed. 

Also, I add a shutdown hook, when the server got interrupt and will shutdown, it can firstly send close message to all clients and close all sockets. This is implemented by starting another thread to keep waiting for interrupt.

------------------------------------------
## TCPClientWithExit
------------------------------------------

Run the class file with two arguments indicating the address and port or without argument(the program will use localhost and default port same as server default port).


If the client does not connect to the server within 14 seconds, the socket will be close, server will send a close message to client, after client tries to send message to server, it will receive the close message and then shutdown. If the server got interrupt such as keyboard interrupt(ctrl + c) and is going to shutdown, it will also send a close message to client.

Example:

    >> javac TCPClientWithExit.java
    >> java TCPClientWithExit 127.0.0.1 1254
    or >> java TCPClientWithExit
    
if you are giving only one argument or more than two argument, the program will ask you to provide two arguments:

    >> java TCPClientWithExit 127.0.0.1
    Please give two arguments
    >>127.0.0.1 1254

then you can provide the string that need to be processed:

    Please give a string: >> This is a TEST
    tset A SI SIHt

you can also open multiple clients to connect to the multithreading server, they work the same and will give you a processed string result. Note that if you are using multiple clients to connect to the same single thread server, only the first client will work, since the server will keep listening to the first client in the main thread and won't be able to listen to other clients, and other clients cannot connect to the server once the server socket is closed, so you'll see an exception when other client tries to connect to the single thread server. After one client has done, open a new server socket.

------------------------------------------
## ServerTask
------------------------------------------

the class extends thread and override run method to interact with client using socket. The processing of string is implemented in this class.

------------------------------------------
## Other Notes
------------------------------------------

Exception in this program is mostly handled by printing error stack trace for debugging unknown bugs. Some exception is handled differently such as SocketTimeoutException. And some serious exception is handled by printing stack trace and forcely shuting down the program. More graceful exception handling will be used in the project1 as requirement. 


Another functionality which need to be noticed is the argument handling. If no valid argument is given, the program will only ask once for valid arguments, otherwise it will throw exception and then exit. Consistently asking for valid argument will be implemented in the project1 as requirement. The other functionality is just as the question title. 
