import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;

import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Utility {
    public static final int PUT = 1;
    public static final int GET = 2;
    public static final int DELETE = 3;

    public static Map<String, Integer> methodMap = new HashMap<String, Integer>(){{
        put("PUT", 1);
        put("GET", 2);
        put("DELETE", 3);
    }};

    public static Pair<String, Error> createRequest(String line) {
        String[] lineArray = line.split(" ");
        if (lineArray.length != 2 && lineArray.length != 3) {
            return new Pair<String, Error>(null, new Error("Invalid operation"));
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = "";

        try {
            String upperArg = lineArray[0].toUpperCase();
            if (upperArg.equals("PUT")) {
                if (lineArray.length == 2) {
                    return new Pair<String, Error>(null,
                            new Error("Usage: PUT <key> <value>"));
                }
                ClientRequest clientRequest = new ClientRequest();
                clientRequest.method = upperArg;
                clientRequest.key = lineArray[1];
                clientRequest.value = lineArray[2];
                requestJson = objectMapper.writeValueAsString(clientRequest);
            } else if (upperArg.equals("GET") || upperArg.equals("DELETE")) {
                if (lineArray.length == 3) {
                    return new Pair<String, Error>(null,
                            new Error("Usage: GET <key> " +
                                    "   Or: DELETE <key>"));
                }
                ClientRequest clientRequest = new ClientRequest();
                clientRequest.method = upperArg;
                clientRequest.key = lineArray[1];
                clientRequest.value = null;
                requestJson = objectMapper.writeValueAsString(clientRequest);
            } else {
                return new Pair<String, Error>(null,
                        new Error("Invalid operation"));
            }
        } catch (JsonProcessingException e) {
            System.err.println(Utility.getDate() + "Cannot process request json: " + e.getMessage());
        }

        return new Pair<String, Error>(requestJson, null);
    }

    public static String getDate() {
        DateFormat simple = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
        Date result = new Date(System.currentTimeMillis());
        return simple.format(result) + " ";
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

    public static String processRequest(String requestString, HashMap<String, String> map) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ClientRequest request = mapper.readValue(requestString, ClientRequest.class);

            switch (methodMap.get(request.method)) {
                case PUT:
                    putByKeyValue(map, request.key, request.value);
                    System.out.printf(Utility.getDate() + "Done putting %s with value %s\n",
                            request.key, request.value);
                    return "Success";

                case GET:
                    Pair valuePair = getByKey(map, request.key);
                    if (valuePair.getValue() == null) {
                        System.out.printf(Utility.getDate() + "Done getting key %s: %s\n",
                                request.key, valuePair.getKey());
                        return valuePair.getKey().toString();
                    } else {
                        System.err.printf(Utility.getDate() + "Error in getting key %s\n",
                                request.key);
                        return valuePair.getValue().toString();
                    }

                case DELETE:
                    if (!deleteByKey(map, request.key)) {
                        System.err.printf(Utility.getDate() + "Fail to delete key %s\n",
                                request.key);
                        return "No such key, failed to delete";
                    } else {
                        System.out.printf(Utility.getDate() + "Done deleting key %s \n",
                                request.key);
                        return "Success";
                    }
            }

        } catch (Exception e) {
            System.err.printf(Utility.getDate() + "Cannot parse request %s: %s\n",
                    requestString, e.getMessage());
        }

        return null;
    }

    private static void putByKeyValue(HashMap<String, String> map, String key, String value) {
        map.put(key, value);
    }

    private static Pair<String, Error> getByKey(HashMap<String, String> map, String key) {
        if (!map.containsKey(key)) {
            return new Pair<String, Error>(null,
                    new Error("Current key-value map does not have such key"));
        }
        return new Pair<String, Error>(map.get(key), null);
    }

    private static boolean deleteByKey(HashMap<String, String> map, String key) {
        if (!map.containsKey(key)) {
            return false;
        }
        map.remove(key);
        return true;
    }

    public static String createResponse(boolean closed, String output) {
        ObjectMapper objectMapper = new ObjectMapper();
        ServerResponse serverResponse = new ServerResponse();
        serverResponse.closed = closed;
        serverResponse.response = output;
        try {
            return objectMapper.writeValueAsString(serverResponse);
        } catch (Exception e) {
            System.err.println(Utility.getDate() + "Cannot process response json: " + e.getMessage());
        }
        return null;
    }

}
