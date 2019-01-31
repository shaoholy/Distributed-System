import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class UDPClient {

    public final Map<String, Integer> methodMap = new HashMap<String, Integer>(){{
        put("PUT", 1);
        put("GET", 2);
        put("DELETE", 3);
    }};

    private Pair<String, Error> createRequest(String line) {
        String[] lineArray = line.split(" ");
        if (lineArray.length != 2 && lineArray.length != 3) {
            return new Pair<String, Error>(null, new Error("Invalid operation"));
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = "";

        try {
            if (lineArray[0].equals("PUT")) {
                if (lineArray.length == 2) {
                    return new Pair<String, Error>(null,
                            new Error("Invalid argument number for operation"));
                }
                ClientRequest clientRequest = new ClientRequest();
                clientRequest.method = methodMap.get(lineArray[0]);
                clientRequest.key = lineArray[1];
                clientRequest.value = lineArray[2];
                requestJson = objectMapper.writeValueAsString(clientRequest);
            } else if (lineArray[0].equals("GET") || lineArray[0].equals("DELETE")) {
                if (lineArray.length == 3) {
                    return new Pair<String, Error>(null,
                            new Error("Invalid argument number for operation"));
                }
                ClientRequest clientRequest = new ClientRequest();
                clientRequest.method = methodMap.get(lineArray[0]);
                clientRequest.key = lineArray[1];
                clientRequest.value = null;
                requestJson = objectMapper.writeValueAsString(clientRequest);
            } else {
                return new Pair<String, Error>(null,
                        new Error("Invalid operation"));
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error in processing request json");
        }

        return new Pair<String, Error>(requestJson, null);
    }

    public void connectToServer(String address, int port) {
        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket();
            InetAddress aHost = InetAddress.getByName(address);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Please give an operation: ");
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    Pair<String, Error> requestPair = createRequest(line);
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
                        System.err.println("Socket is closed, client exiting...");
                        break;
                    }
                    System.out.println("Reply: " + response.response);
                }
            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null) {
                aSocket.close();
            }
        }
    }

    public static Pair<Boolean, Integer> isPortValid(String portString) {
        int port;
        try {
            port = Integer.parseInt(portString);
            if (port > 65535 || port < 0) {
                return new Pair<Boolean, Integer>(false, 0);
            }
        } catch (Exception e) {
            return new Pair<Boolean, Integer>(false, 0);
        }
        return new Pair<Boolean, Integer>(true, port);
    }

    public static void main(String args[]) {
        int port;
        String address;
        Pair<Boolean, Integer> portPair;

        if (args.length == 2 && (portPair = isPortValid(args[1])).getKey()) {
            address = args[0];
            port = portPair.getValue();
        } else {
            System.out.println("Please give two arguments, " +
                    "address and port, separate with space");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] line_arg = line.trim().split("\\s+");
                    if (line_arg.length == 2 && (portPair = isPortValid(line_arg[1])).getKey()) {
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