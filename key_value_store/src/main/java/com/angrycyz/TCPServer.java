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

public class TCPServer implements Server {
    private static final Logger logger = LogManager.getLogger("TCPServer");

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
        BufferedReader bufferedReader = null;
        PrintWriter printWriter = null;
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                printWriter = new PrintWriter(socket.getOutputStream(), true);

                String inputString;
                while ((inputString = bufferedReader.readLine()) != null) {
                    logger.debug(formatTCPRequest(inputString, socket));
                    /* process request and get results
                     * results could be either the real result of hashmap operation
                     * or success/failure message
                     */
                    String response = Utility.processRequest(inputString, map);
                    /* send to client */
                    printWriter.println(response);
                    logger.debug(map);
                }

            }
        } catch (SocketException e) {
            logger.error("Socket: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            logger.error("IO: " + e.getMessage());
            System.exit(1);
        } finally {
            try {
                if (printWriter != null) {
                    printWriter.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (Exception e) {
            logger.error("Cannot close serverSocket: " + e.getMessage());
            System.exit(1);
        }
        }
    }
}
