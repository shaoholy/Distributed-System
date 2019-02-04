package com.angrycyz;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TCPServer implements Server {

    private void closeTCPSocket(Socket s) {
        if (s != null && !s.isClosed()) {
            try {
                s.close();
            } catch (Exception e) {
                System.err.println(Utility.getDate() + "Cannot close socket: " + e.getMessage());
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
            System.err.println(Utility.getDate() + "Cannot send close response to client: " + e.getMessage());
        }
    }

    public static String formatTCPRequest(String requestString, Socket socket) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ClientRequest request = objectMapper.readValue(requestString, ClientRequest.class);
            if (socket != null && !socket.isClosed()) {
                return String.format("Request from %s:%d: %s %s %s\n",
                        socket.getInetAddress(), socket.getPort(),
                        request.method, request.key, request.value);
            }
            return String.format("Request: %s %s %s\n",
                    request.method, request.key, request.value);
        } catch (Exception e) {
            System.err.println(Utility.getDate() + "Cannot format request: " + e.getMessage());
        }
        return null;
    }

    public void startServer(int port) {
        HashMap<String, String> map = new HashMap<String, String>();
        final List<Socket> globalSockets = new ArrayList<Socket>();

        /* add a shutdown hook to send close message
         * to client and do the cleanup
         */
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                for (Socket s : globalSockets) {
                    sendTCPCloseResponse(s);
                    closeTCPSocket(s);
                }
                System.out.println(Utility.getDate() + "Keyboard Interrupt, Shutdown...");
            }
        });

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                globalSockets.add(socket);
                // close server socket to make sure that other client throw exception
                serverSocket.close();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

                String inputString;
                while ((inputString = bufferedReader.readLine()) != null) {
                    System.out.printf(Utility.getDate() + formatTCPRequest(inputString, socket));

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
                globalSockets.remove(globalSockets.size() - 1);
                // open a new server socket
                serverSocket = new ServerSocket(port);
            }
        } catch (SocketException e) {
            System.err.println(Utility.getDate() + "Socket: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println(Utility.getDate() + "IO: " + e.getMessage());
            System.exit(1);
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    System.err.println(Utility.getDate() + "Cannot close serverSocket: " + e.getMessage());
                    System.exit(1);
                }
            }
        }
    }
}
