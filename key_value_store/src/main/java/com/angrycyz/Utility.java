package com.angrycyz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


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

    private static final Logger logger = LogManager.getLogger("Utility");


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

    public static Pair<String[], Error> getValidRequest(String rawRequest) {
        String[] requestArray = rawRequest.split(" ");
        if (requestArray.length != 2 && requestArray.length != 3) {
            logger.debug("Invalid Operation: " +
                    rawRequest);
            return new Pair<String[], Error>(null, new Error("Usage: PUT <key> <value>\n" +
                    "   Or: GET <key>\n" +
                    "   Or: DELETE <key>"));
        }

        String upperRequest = requestArray[0].toUpperCase();

        if (upperRequest.equals("PUT")) {
            if (requestArray.length == 2) {
                logger.debug("Invalid arguments: " +
                        rawRequest);
                return new Pair<String[], Error>(null,
                        new Error("Usage: PUT <key> <value>"));
            }

            return new Pair<String[], Error>(
                    new String[]{upperRequest, requestArray[1], requestArray[2]},
                    null);
        } else if (upperRequest.equals("GET") || upperRequest.equals("DELETE")) {
            if (requestArray.length == 3) {
                logger.debug("Invalid arguments: " +
                        rawRequest);
                return new Pair<String[], Error>(null,
                        new Error("Usage: GET <key> \n" +
                                "   Or: DELETE <key>"));
            }
            return new Pair<String[], Error>(
                    new String[]{upperRequest, requestArray[1]},
                    null);
        }

        logger.debug("Invalid Operation: " +
                rawRequest);
        return new Pair<String[], Error>(null,
                new Error("Usage: PUT <key> <value>\n" +
                        "   Or: GET <key>\n" +
                        "   Or: DELETE <key>"));
    }


    public static String processRequest(String requestString, HashMap<String, String> map) {

        Pair<String[], Error> requestPair = getValidRequest(requestString);
        if (requestPair.getValue() != null) {
            return requestPair.getValue().getMessage();
        }
        String[] request = requestPair.getKey();

        switch (methodMap.get(request[0])) {
            case PUT:
                putByKeyValue(map, request[1], request[2]);
                logger.debug("Done putting " + request[1] +
                        " with value " + request[2]);
                return "Success";

            case GET:
                Pair valuePair = getByKey(map, request[1]);
                if (valuePair.getValue() == null) {
                    logger.debug("Done getting key " +
                            request[1] + ": " + valuePair.getKey());
                    return valuePair.getKey().toString();
                } else {
                    logger.debug("Failed to get key " +
                            request[1]);
                    return "No such key, failed to get";
                }

            case DELETE:
                if (!deleteByKey(map, request[1])) {
                    logger.debug("Fail to delete key " +
                            request[1]);
                    return "No such key, failed to delete";
                } else {
                    logger.debug("Done deleting key " +
                            request[1]);
                    return "Success";
                }
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
            logger.error("Cannot process response json: " + e.getMessage());
        }
        return null;
    }

}
