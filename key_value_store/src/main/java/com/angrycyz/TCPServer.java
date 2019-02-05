package com.angrycyz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class TCPServer implements Server {
    private static final Logger logger = LogManager.getLogger("TCPServer");

    private void closeTCPSocket(Socket s) {
        if (s != null && !s.isClosed()) {
            try {
                s.close();
            } catch (Exception e) {
                logger.error("Cannot close socket: " + e.getMessage());
            }
        }
    }

    private void sendTCPCloseResponse(Socket socket) {
        try {
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            /* set field "closed" in response as true, message as null */
            printWriter.println(Utility.createResponse(true, null));
            printWriter.close();
        } catch (Exception e) {
            logger.error("Cannot send close response to client: " + e.getMessage());
        }
    }

    public static String formatTCPRequest(String requestString, Socket socket) {

        if (socket != null && !socket.isClosed()) {
            return String.format("Request from %s:%d: %s",
                    socket.getInetAddress(), socket.getPort(),
                    requestString);
        }
        return String.format("Request: %s %s",
                requestString);
    }

    public void startServer(HashMap<String, String> map, int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                // close server socket to make sure that other client throw exception
                serverSocket.close();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

                String inputString;
                while ((inputString = bufferedReader.readLine()) != null) {
                    logger.debug(formatTCPRequest(inputString, socket));

                    /* process request and get results
                     * results could be either the real result of hashmap operation
                     * or success/failure message
                     */
                    String output = Utility.processRequest(inputString, map);
                    /* create a response json string(closed field set to false) */
                    String response = Utility.createResponse(false, output);
                    /* send to client */
                    printWriter.println(response);
                }
                printWriter.close();
                bufferedReader.close();
                closeTCPSocket(socket);
                // open a new server socket
                serverSocket = new ServerSocket(port);
            }
        } catch (SocketException e) {
            logger.error("Socket: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            logger.error("IO: " + e.getMessage());
            System.exit(1);
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    logger.error("Cannot close serverSocket: " + e.getMessage());
                    System.exit(1);
                }
            }
        }
    }
}
