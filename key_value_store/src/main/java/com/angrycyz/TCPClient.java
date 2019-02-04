package com.angrycyz;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class TCPClient {

    public final int TIME_LIMIT = 20 * 1000;

    public void connectToServer(String address, int port) {
        Socket socket = null;
        try {
            socket = new Socket(address, port);
            socket.setSoTimeout(TIME_LIMIT);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);
            /* keep scanning input from console */
            while (true) {
                System.out.print(Utility.getDate() + "Please give an operation: ");
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    Pair<String, Error> requestPair = Utility.createRequest(line);
                    Error err;
                    if ((err = requestPair.getValue()) != null) {
                        System.err.println(err.getMessage());
                        System.out.printf("Usage: PUT <key> <value>\n" +
                                "   Or: GET <key>\n" +
                                "   Or: DELETE <key>\n");
                        continue;
                    }
                    printWriter.println(requestPair.getKey());
                    String str;
                    if ((str = bufferedReader.readLine()) != null) {
                        /* if client receive close message from server,
                         * client will exit
                         */
                        ObjectMapper objectMapper = new ObjectMapper();
                        ServerResponse response = objectMapper.readValue(str, ServerResponse.class);
                        if (response.closed) {
                            System.err.println(Utility.getDate() + "Socket is closed, client exiting...");
                            break;
                        }
                        System.out.println(response.response);
                    }

                }
            }

            printWriter.close();
            bufferedReader.close();

        } catch (SocketException e) {
            System.err.println(Utility.getDate() + "Socket: " + e.getMessage());
        } catch (SocketTimeoutException e) {
            System.err.println(Utility.getDate() + "Timeout: " + e.getMessage());
        } catch (IOException e) {
            System.err.println(Utility.getDate() + "IO: " + e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Cannot close socket: " + e.getMessage());
                }
            }
        }

    }
    public static void main(String[] args) {
        int port;
        String address;
        Pair<Boolean, Integer> portPair;

        if (args.length == 2 && (portPair = Utility.isPortValid(args[1])).getKey()) {
            address = args[0];
            port = portPair.getValue();
        } else {
            System.out.println(Utility.getDate() + "Please give two arguments, " +
                    "address and port, separate with space");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] line_arg = line.trim().split("\\s+");
                    if (line_arg.length == 2 && (portPair = Utility.isPortValid(line_arg[1])).getKey()) {
                        address = line_arg[0];
                        port = portPair.getValue();
                        break;
                    }
                }
            }
        }

        TCPClient client = new TCPClient();
        client.connectToServer(address, port);
    }
}