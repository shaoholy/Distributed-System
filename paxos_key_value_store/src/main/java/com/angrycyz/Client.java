package com.angrycyz;

import com.angrycyz.grpc.KeyRequest;
import com.angrycyz.grpc.KeyValueRequest;
import com.angrycyz.grpc.KeyValueStoreGrpc;
import com.angrycyz.grpc.OperationReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client {
    private final ManagedChannel channel;
    private final KeyValueStoreGrpc.KeyValueStoreBlockingStub blockingStub;
    private final int STUB_TIMEOUT = 3;
    private String connectedPort;
    private static final Logger logger = LogManager.getLogger("Client");

    public Client(String address, int port) {
        this(ManagedChannelBuilder.forAddress(address, port)
                .usePlaintext().build());
        connectedPort = Integer.toString(port);
    }

    Client(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = KeyValueStoreGrpc.newBlockingStub(channel);
    }

    public void sendOperation() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
//            logger.info("Please give Operation: ");
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.equals("EOF")) {
                    System.exit(1);
                }
                logger.info(connectedPort + " Given operation:" + line);
                Pair<String[], Error> pair = processRequest(line);
                if (pair.getValue() != null) {
                    logger.error(pair.getValue().getMessage() +
                            "\nUsage: PUT <key> <value>" +
                            " Or: GET <key>" +
                            " Or: DELETE <key>");
                } else {
                    OperationReply reply = null;

                    if (pair.getKey()[0].equals("PUT")) {
                        reply = sendPutOperation(pair.getKey()[1], pair.getKey()[2]);
                    } else if (pair.getKey()[0].equals("GET")) {
                        reply = sendGetOperation(pair.getKey()[1]);
                    } else if (pair.getKey()[0].equals("DELETE")) {
                        reply = sendDeleteOperation(pair.getKey()[1]);
                    } else if (pair.getKey()[0].equals("CLEARALL")) {
                        reply = sendClearAllOperation();
                    }
                    if (reply != null) {
                        logger.info(connectedPort + " Server Response: " + reply.getReply());
                    }
                }
            }
        }
    }

    public OperationReply sendClearAllOperation() {
        KeyRequest request = KeyRequest
                .newBuilder()
                .build();
        try {
            return blockingStub.withDeadlineAfter(STUB_TIMEOUT, TimeUnit.SECONDS).mapClearAll(request);
        } catch (Exception e) {
            logger.error("Cannot clear all : " + e.getMessage());
        }
        return null;
    }

    public OperationReply sendGetOperation(String key) {
        KeyRequest request = KeyRequest
                .newBuilder()
                .setKey(key)
                .build();
        try {
            return blockingStub.withDeadlineAfter(STUB_TIMEOUT, TimeUnit.SECONDS).mapGet(request);
        } catch (Exception e) {
            logger.error("Cannot GET from server: " + e.getMessage());
        }
        return null;
    }

    public OperationReply sendDeleteOperation(String key) {
        KeyRequest request = KeyRequest
                .newBuilder()
                .setKey(key)
                .build();
        try {
            return blockingStub.withDeadlineAfter(STUB_TIMEOUT, TimeUnit.SECONDS).mapDelete(request);
        } catch (Exception e) {
            logger.error("Cannot GET from server: " + e.getMessage());
        }
        return null;
    }

    public OperationReply sendPutOperation(String key, String value) {
        KeyValueRequest request = KeyValueRequest
                .newBuilder()
                .setKey(key)
                .setValue(value)
                .build();
        try {
            return blockingStub.withDeadlineAfter(STUB_TIMEOUT, TimeUnit.SECONDS).mapPut(request);
        } catch (Exception e) {
            logger.error("Cannot GET from server: " + e.getMessage());
        }
        return null;
    }

    private Pair<String[], Error> processRequest(String request) {
        String[] requestArray = request.split(" ");
        if (requestArray.length > 3) {
            return new Pair<String[], Error>(null, new Error("Invalid Operation"));
        }

        /* convert to uppercase to make sure all case of operations work */
        String upperRequest = requestArray[0].toUpperCase();

        /* check if the operation is one of following */
        if (upperRequest.equals("PUT")) {
            if (requestArray.length <= 2) {
                return new Pair<String[], Error>(null,
                        new Error("Invalid Operation"));
            }

            return new Pair<String[], Error>(
                    new String[]{upperRequest, requestArray[1], requestArray[2]},
                    null);
        } else if (upperRequest.equals("GET") || upperRequest.equals("DELETE")) {
            if (requestArray.length != 2) {
                return new Pair<String[], Error>(null,
                        new Error("Invalid Operation"));
            }
            return new Pair<String[], Error>(
                    new String[]{upperRequest, requestArray[1]},
                    null);
        } else if (upperRequest.equals("CLEARALL")) {
            if (requestArray.length != 1) {
                return new Pair<String[], Error>(null,
                        new Error("Invalid Operation"));
            }
            return new Pair<String[], Error>(
                    new String[]{upperRequest},
                    null);
        }

        return new Pair<String[], Error>(null,
                new Error("Invalid Operation"));
    }

    public static void main(String[] args) {
        int port;
        String address;
        Pair<Boolean, Integer> portPair;

        /* set log configuration file path */
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        String propLocation = "src/main/resources/log4j2.xml";
        File file = new File(propLocation);

        context.setConfigLocation(file.toURI());

        logger.info("Log properties file location: " + propLocation);

        /* ask user for address and port */
        if (args.length == 2 && (portPair = Utility.isPortValid(args[1])).getKey()) {
            address = args[0];
            port = portPair.getValue();
        } else {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                logger.info("Please give two arguments, " +
                        "address and port, separate with space");
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

        Client client = new Client(address, port);
        client.sendOperation();

    }
}