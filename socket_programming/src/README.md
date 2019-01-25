
------------------------------------------
## MultiThreadingTCPServer
------------------------------------------

Simply run the class file with one argument indicating the port or with no argument(the program will use a default port).

You can use ctrl+c to shutdown the server, and the server will send a close message to clients. 

When timed out, server will print thread done and socket close message.

------------------------------------------
## TCPClientWithExit
------------------------------------------

Run the class file with two arguments indicating the address and port or without argument(the program will use localhost and default port same as server default port).


If the client does not connect to the server within 14 seconds, the socket will be close, server will send a close message to client, after client tries to send message to server, it will receive the close message and then shutdown. If the server got interrupt such as keyboard interrupt(ctrl + c) and is going to shutdown, it will also send a close message to client.

------------------------------------------
## ServerTask
------------------------------------------

the class extends thread and override run method to interact with client with socket.
