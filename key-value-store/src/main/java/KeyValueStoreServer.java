import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class KeyValueStoreServer {

    public static final String TCP_COMM = "TCP";
    public static final String UDP_COMM = "UDP";
    private final int PUT = 1;
    private final int GET = 2;
    private final int DELETE = 3;

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

    public ServerConfig parseConfig(String configPath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String configStr = new String(Files.readAllBytes(Paths.get(configPath)));
            return mapper.readValue(configStr, ServerConfig.class);
        } catch (Exception e) {
//            e.printStackTrace();
            System.err.printf("Cannot parse config file %s\n", configPath);
        }
        return null;
    }

    public String processRequest(String requestString, HashMap<String, String> map) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ClientRequest request = mapper.readValue(requestString, ClientRequest.class);

            switch (request.method) {
                case PUT:
                    putByKeyValue(map, request.key, request.value);
                    System.out.printf("Done putting %s with value %s\n",
                            request.key, request.value);
                    return "Success";

                case GET:
                    Pair valuePair = getByKey(map, request.key);
                    if (valuePair.getValue() == null) {
                        System.out.printf("Done getting key %s: %s\n",
                                request.key, valuePair.getKey());
                        return valuePair.getKey().toString();
                    } else {
                        System.err.printf("Error in getting key %s\n",
                                request.key);
                        return valuePair.getValue().toString();
                    }

                case DELETE:
                    if (!deleteByKey(map, request.key)) {
                        System.err.printf("Fail to delete key %s\n",
                                request.key);
                        return "No such key, failed to delete";
                    } else {
                        System.out.printf("Done deleting key %s \n",
                                request.key);
                        return "Success";
                    }
            }

        } catch (Exception e) {
//            e.printStackTrace();
            System.err.printf("Cannot parse request: %s\n", requestString);
        }

        return null;
    }

    private void putByKeyValue(HashMap<String, String> map, String key, String value) {
        map.put(key, value);
    }

    private Pair<String, Error> getByKey(HashMap<String, String> map, String key) {
        if (!map.containsKey(key)) {
            return new Pair<String, Error>(null, new Error("Hashmap does not have such key"));
        }
        return new Pair<String, Error>(map.get(key), null);
    }

    private boolean deleteByKey(HashMap<String, String> map, String key) {
        if (!map.containsKey(key)) {
            return false;
        }
        map.remove(key);
        return true;
    }

    private String createResponse(boolean closed, String output) {
        ObjectMapper objectMapper = new ObjectMapper();
        ServerResponse serverResponse = new ServerResponse();
        serverResponse.closed = closed;
        serverResponse.response = output;
        try {
            return objectMapper.writeValueAsString(serverResponse);
        } catch (Exception e) {
            System.err.println("Error in processing response json");
        }
        return null;
    }

    public static void closeTCPSocket(Socket s) {
        if (!s.isClosed()) {
            try {
                s.close();
            } catch (Exception e) {
                System.err.println("Error in closing socket");
            }
        }
    }

    public void sendTCPCloseResponse(Socket socket) {
        try {
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            printWriter.println(createResponse(true, null));
            printWriter.close();
        } catch (Exception e) {
            System.err.println("Cannot send close response to client");
        }
    }

    public String getDate() {
        DateFormat simple = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
        Date result = new Date(System.currentTimeMillis());
        return simple.format(result) + " ";
    }

    public void startTCPServer(int port) {
        HashMap<String, String> map = new HashMap<String, String>();
        final List<Socket> globalSockets = new ArrayList<Socket>();

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                for (Socket s : globalSockets) {
                    sendTCPCloseResponse(s);
                    closeTCPSocket(s);
                }
                System.out.println(getDate() + "Keyboard Interrupt, Shutdown...");
            }
        });

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            /* set 2 minute timeout */
            serverSocket.setSoTimeout(2 * 60 * 1000);
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    // close server socket to make sure that other client throw exception
                    globalSockets.add(socket);
                    serverSocket.close();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

                    String inputString;
                    while ((inputString = bufferedReader.readLine()) != null) {
                        System.out.printf("From Client: %s\n", inputString);

                        String output = processRequest(inputString, map);
                        String response = createResponse(false, output);
                        printWriter.println(response);
                    }
                    printWriter.close();
                    bufferedReader.close();
                    closeTCPSocket(socket);
                    globalSockets.remove(globalSockets.size() - 1);
                    // open a new server socket
                    serverSocket = new ServerSocket(port);
                } catch (Exception e) {
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void startUDPServer(int port) {
        HashMap<String, String> map = new HashMap<String, String>();
        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket(port);
            byte[] buffer = new byte[1000];
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                System.out.printf("From Client: %s\n", new String(request.getData()));
                String output = processRequest(new String(request.getData()), map);
                String response = createResponse(false, output);
                byte[] responseByt = response.getBytes();
                DatagramPacket reply = new DatagramPacket(responseByt,
                        response.length(), request.getAddress(),
                        request.getPort());
                aSocket.send(reply);
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

    public static void main(String[] args) {
        int port;
        Pair<Boolean, Integer> portPair;

        if (args.length == 1 && (portPair = isPortValid(args[0])).getKey()) {
            port = portPair.getValue();
        } else {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("Please give one valid port number");
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] line_arg = line.trim().split("\\s+");
                    if (line_arg.length == 1 && (portPair = isPortValid(line_arg[0])).getKey()) {
                        port = portPair.getValue();
                        break;
                    }
                }
            }

        }

        System.out.printf("Using the port %d\n", port);
        KeyValueStoreServer server = new KeyValueStoreServer();
        ServerConfig serverConfig = server.parseConfig("etc/comm_config.json");

        if (serverConfig.comm.equals(TCP_COMM)) {
            System.out.println("Starting TCP server");
            server.startTCPServer(port);
        } else if (serverConfig.comm.equals(UDP_COMM)) {
            System.out.println("Starting UDP server");
            server.startUDPServer(port);
        } else {
            System.out.printf("%s is not valid communication protocol," +
                    "Please check config file\n", serverConfig.comm);
            System.exit(1);
        }
    }
}