
------------------------------------------
## MultiThreadingTCPServer
------------------------------------------

Simply run the class file with one argument indicating the port or with no argument(the program will use a default port).

You can use ctrl+c to shutdown the server, and the server will send a close message to clients. 

When timed out, server will print thread done and socket close message.

Example: 

    *>> javac MultiThreadingTCPServer.java*
    *>> java MultiThreadingTCPServer*
    or *>> java MultiThreadingTCPServer 1254*
    
if you are giving more than one argument, the program will ask you to provide only one agument.

Example:

    *>> java MultiThreadingTCPServer 1254 1222*
    *Please give only one argument*
    *>> 1254*

------------------------------------------
## TCPClientWithExit
------------------------------------------

Run the class file with two arguments indicating the address and port or without argument(the program will use localhost and default port same as server default port).


If the client does not connect to the server within 14 seconds, the socket will be close, server will send a close message to client, after client tries to send message to server, it will receive the close message and then shutdown. If the server got interrupt such as keyboard interrupt(ctrl + c) and is going to shutdown, it will also send a close message to client.

Example:

    *>> javac TCPClientWithExit.java*
    *>> java TCPClientWithExit 127.0.0.1 1254*
    or *>> java TCPClientWithExit*
    
if you are giving only one argument or more than two argument, the program will ask you to provide two arguments:

    *>> java TCPClientWithExit 127.0.0.1*
    *Please give two arguments*
    *>>127.0.0.1 1254*

then you can provide the string that need to be processed:

    *Please give a string: >> This is a TEST*
    *tset A SI SIHt*

------------------------------------------
## ServerTask
------------------------------------------

the class extends thread and override run method to interact with client using socket.
