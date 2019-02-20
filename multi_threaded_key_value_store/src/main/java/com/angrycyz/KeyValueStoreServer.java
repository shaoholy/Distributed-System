package com.angrycyz;

import com.angrycyz.grpc.KeyRequest;
import com.angrycyz.grpc.KeyValueRequest;
import com.angrycyz.grpc.KeyValueStoreGrpc;
import com.angrycyz.grpc.OperationReply;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KeyValueStoreServer {

    private static final Logger logger = LogManager.getLogger("KeyValueStoreServer");

    private Server server;

    private static ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();

    static class KeyValueStoreImpl extends KeyValueStoreGrpc.KeyValueStoreImplBase {
        @Override
        public void mapGet(KeyRequest request, StreamObserver<OperationReply> responseObserver) {
            OperationReply reply;

            String key = request.getKey();
            logger.info("Get " + key);
            if (map.containsKey(key)) {
                reply = OperationReply.newBuilder().setReply(map.get(key)).build();
            } else {
                reply = OperationReply.newBuilder().setReply("Key does not exist.").build();
            }
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void mapPut(KeyValueRequest request, StreamObserver<OperationReply> responseObserver) {
            String key = request.getKey();
            String value = request.getValue();
            logger.info("Put " + key + " " + value);
            map.put(key, value);
            OperationReply reply = OperationReply.newBuilder().setReply("Success").build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void mapDelete(KeyRequest request, StreamObserver<OperationReply> responseObserver) {
            OperationReply reply;

            String key = request.getKey();
            logger.info("Delete " + key);
            if (map.containsKey(key)) {
                reply = OperationReply.newBuilder().setReply(map.remove(key)).build();
            } else {
                reply = OperationReply.newBuilder().setReply("Key does not exist.").build();
            }
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    public void startServer(int port) {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                KeyValueStoreServer.this.stop();
                logger.info("Keyboard Interrupt, Shutdown...");
            }
        });

        try {
            this.server = ServerBuilder.forPort(port)
                    .addService(new KeyValueStoreImpl())
                    .build()
                    .start();
            logger.info("Started server, listening on " + Integer.toString(port));
        } catch (IOException e) {
            logger.error("IO: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    private void stop() {
        if (this.server != null) {
            this.server.shutdown();
        }
    }

    private void blockUntilShutdown() {
        try {
            if (server != null) {
                server.awaitTermination();
            }
        } catch (InterruptedException e ) {
            logger.error("Interrupted: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port;
        Pair<Boolean, Integer> portPair;

        /* set log configuration file location */
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        String propLocation = "src/main/resources/log4j2.xml";
        File file = new File(propLocation);

        context.setConfigLocation(file.toURI());

        logger.info("Log properties file location: " + propLocation);

        /* get valid port number */
        if (args.length == 1 && (portPair = Utility.isPortValid(args[0])).getKey()) {
            port = portPair.getValue();
        } else {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                logger.info("Please give one valid port number");
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] line_arg = line.trim().split("\\s+");
                    if (line_arg.length == 1 && (portPair = Utility.isPortValid(line_arg[0])).getKey()) {
                        port = portPair.getValue();
                        break;
                    }
                }
            }

        }
        /* pre populate map with 15 key-value pairs */
        int ascii_a = (int)'a';

        for (int i = 0; i < 15; i++) {
            map.put(Character.toString((char)(ascii_a + i)), Integer.toString(i));
        }
        logger.info(map);

        KeyValueStoreServer server = new KeyValueStoreServer();

        server.startServer(port);
        server.blockUntilShutdown();

    }
}