package com.angrycyz;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class UDPClient {

    public final int TIME_LIMIT = 20 * 1000;

    public void connectToServer(String address, int port) {
        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket();
            aSocket.setSoTimeout(TIME_LIMIT);
            InetAddress aHost = InetAddress.getByName(address);
            Scanner scanner = new Scanner(System.in);
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
                    byte[] msg = requestPair.getKey().getBytes();
                    DatagramPacket request = new DatagramPacket(msg,
                            requestPair.getKey().length(), aHost, port);
                    aSocket.send(request);

                    byte[] buffer = new byte[1000];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(reply);
                    String replyStr = new String(reply.getData());
                    ObjectMapper objectMapper = new ObjectMapper();
                    ServerResponse response = objectMapper.readValue(replyStr, ServerResponse.class);
                    if (response.closed) {
                        System.err.println(Utility.getDate() + "Socket is closed, client exiting...");
                        break;
                    }
                    System.out.println(Utility.getDate() + "Reply: " + response.response);
                }
            }
        } catch (SocketException e) {
            System.err.println(Utility.getDate() + "Socket: " + e.getMessage());
        } catch (SocketTimeoutException e) {
                System.err.println(Utility.getDate() + "Timeout: " + e.getMessage());
        } catch (IOException e) {
            System.err.println(Utility.getDate() + "IO: " + e.getMessage());
        } finally {
            if (aSocket != null) {
                aSocket.close();
            }
        }
    }

    public static void main(String args[]) {
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

        UDPClient client = new UDPClient();
        client.connectToServer(address, port);
    }
}