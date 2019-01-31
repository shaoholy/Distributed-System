import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TCPClient {
    public final Map<String, Integer> methodMap = new HashMap<String, Integer>(){{
        put("PUT", 1);
        put("GET", 2);
        put("DELETE", 3);
    }};

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
        try {
            Socket socket = new Socket(address, port);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);
            /* keep scanning input from console */
            while (true) {
                System.out.print("Please give an operation: ");
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    Pair<String, Error> requestPair = createRequest(line);
                    Error err;
                    if ((err = requestPair.getValue()) != null) {
                        System.err.println(err.getMessage());
                        continue;
                    }
                    printWriter.println(requestPair.getKey());
                    String str;
                    if ((str = bufferedReader.readLine()) != null) {
                        /* if server send a close message,
                         * client will exit
                         */
                        ObjectMapper objectMapper = new ObjectMapper();
                        ServerResponse response = objectMapper.readValue(str, ServerResponse.class);
                        if (response.closed) {
                            System.err.println("Socket is closed, client exiting...");
                            break;
                        }
                        System.out.println(response.response);
                    }

                }
            }

            printWriter.close();
            bufferedReader.close();
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
    public static void main(String[] args) {
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

        TCPClient client = new TCPClient();
        client.connectToServer(address, port);
    }
}