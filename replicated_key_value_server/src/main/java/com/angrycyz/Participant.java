package com.angrycyz;

import com.angrycyz.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Participant{
    private static final Logger logger = LogManager.getLogger("Participant");
    private int port;
    private Server server;
    private ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
    private List<UndoLog> undoLogs = new ArrayList<UndoLog>();
    private List<RedoLog> redoLogs = new ArrayList<RedoLog>();
    private final int STUB_TIMEOUT = 3;
    private String ip;
    private ManagedChannel channel;
    private KeyValueStoreGrpc.KeyValueStoreBlockingStub blockingStub;
    private CoordinationGrpc.CoordinationBlockingStub cBlockingStub;
    private Pair<String, Integer> coordinatorConfig;

    private class UndoLog {

        public UndoLog(String transactionId, String operation, String prevKey, String prevVal) {
            this.transactionId = transactionId;
            this.operation = operation;
            this.prevKey = prevKey;
            this.prevVal = prevVal;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getPrevKey() {
            return prevKey;
        }

        public void setPrevKey(String prevKey) {
            this.prevKey = prevKey;
        }

        public String getPrevVal() {
            return prevVal;
        }

        public void setPrevVal(String prevVal) {
            this.prevVal = prevVal;
        }

        private String transactionId;
        private String operation;
        private String prevKey;
        private String prevVal;
    }

    private class RedoLog {
        public RedoLog(String transactionId, String operation, String key, String value) {
            this.transactionId = transactionId;
            this.operation = operation;
            this.key = key;
            this.value = value;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        private String transactionId;
        private String operation;
        private String key;
        private String value;
    }

    class ParticipationImpl extends ParticipationGrpc.ParticipationImplBase {
        @Override
        public void canCommit(VoteRequest voteRequest, StreamObserver<VoteReply> responseObserver) {
            String operation = voteRequest.getOperation();
            String key = voteRequest.getKey();
            String value = voteRequest.getValue();
            String tId = voteRequest.getTransactionId();

            if (map.containsKey(key)) {
                undoLogs.add(new UndoLog(tId, operation, key, map.get(value)));
                redoLogs.add(new RedoLog(tId, operation, key, map.get(value)));
            } else {
                undoLogs.add(new UndoLog(tId, operation, key, null));
                redoLogs.add(new RedoLog(tId, operation, key, null));
            }

            VoteReply voteReply;

            if (operation.equals("PUT")) {
                map.put(key, value);
                voteReply = VoteReply.newBuilder()
                        .setVote(true)
                        .build();
            } else if (operation.equals("DELETE")) {
                map.remove(key);
                voteReply = VoteReply.newBuilder()
                        .setVote(true)
                        .build();
            } else {
                voteReply = VoteReply.newBuilder()
                        .setVote(false)
                        .build();
            }

            responseObserver.onNext(voteReply);
            responseObserver.onCompleted();
        }

        @Override
        public void doCommit(CommitRequest commitRequest, StreamObserver<AckMessage> responseObserver) {
            /* confirm transaction is committed */
            ConfirmRequest confirmRequest = ConfirmRequest.newBuilder()
                    .setTransactionId(commitRequest.getTransactionId())
                    .setParticipantAddress(ip)
                    .setParticipantPort(port)
                    .build();

            ConfirmReply confirmReply = cBlockingStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .haveCommitted(confirmRequest);

            AckMessage ackMessage;
            if (confirmReply.getCommitted()) {
                ackMessage = AckMessage.newBuilder()
                        .setMsg("Committed")
                        .build();
            } else {
                ackMessage = AckMessage.newBuilder()
                        .setMsg("Require commit of coordinator")
                        .build();
                logger.error("Coordinator did not commit");
            }

            responseObserver.onNext(ackMessage);
            responseObserver.onCompleted();
        }

        @Override
        public void doAbort(RollbackRequest rollbackRequest, StreamObserver<AckMessage> responseObserver) {
            String tId = rollbackRequest.getTransactionId();
            int undoSize = undoLogs.size();
            /* find the undo log, reset to previous status*/
            UndoLog undo = undoLogs.get(undoSize - 1);
            if (undo.getTransactionId().equals(tId)) {
                /* if map contains the key
                 * means the operation is put
                 * otherwise its delete
                 */
                String key = undo.getPrevKey();
                if (map.containsKey(key)) {
                    /* value = null means before the commit,
                     * there's no such key
                     */
                    if (undo.getPrevVal() == null) {
                        map.remove(key);
                    } else {
                        map.put(key, undo.getPrevVal());
                    }
                } else {
                    map.put(key, undo.getPrevVal());
                }
            }

            AckMessage ackMessage = AckMessage.newBuilder()
                    .setMsg("Aborted")
                    .build();

            responseObserver.onNext(ackMessage);
            responseObserver.onCompleted();

        }
    }

    class KeyValueStoreImpl extends KeyValueStoreGrpc.KeyValueStoreImplBase {
        @Override
        public void mapGet(KeyRequest kvRequest, StreamObserver<OperationReply> responseObserver) {
            String key = kvRequest.getKey();
            OperationReply kvReply;
            logger.info("Get " + key);

            if (map.containsKey(key)) {
                logger.info(map.get(key));
                kvReply = OperationReply.newBuilder()
                        .setReply(map.get(key))
                        .build();
            } else {
                kvReply = OperationReply.newBuilder()
                        .setReply("Key does not exist")
                        .build();
            }

            responseObserver.onNext(kvReply);
            responseObserver.onCompleted();
        }

        @Override
        public void mapPut(KeyValueRequest kvRequest, StreamObserver<OperationReply> responseObserver) {
            try {
                OperationReply kvReply = blockingStub
                        .withDeadlineAfter(STUB_TIMEOUT, TimeUnit.SECONDS)
                        .mapPut(kvRequest);

                responseObserver.onNext(kvReply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Cannot get from coordinator: " + e.getMessage());
            }
        }

        @Override
        public void mapDelete(KeyRequest kvRequest, StreamObserver<OperationReply> responseObserver) {
            try {
                OperationReply kvReply = blockingStub
                        .withDeadlineAfter(STUB_TIMEOUT, TimeUnit.SECONDS)
                        .mapDelete(kvRequest);

                responseObserver.onNext(kvReply);
                responseObserver.onCompleted();
            } catch (Exception e) {
            logger.error("Cannot get from coordinator: " + e.getMessage());
            }
        }
    }

    public Participant(int port) {
        this.port = port;
    }


    public void initializeChannel() {
        buildChannels(ManagedChannelBuilder
                .forAddress(coordinatorConfig.getKey(), coordinatorConfig.getValue())
                .usePlaintext()
                .build());
    }

    private void buildChannels(ManagedChannel channel) {
        this.channel = channel;
        this.blockingStub = KeyValueStoreGrpc.newBlockingStub(channel);
        this.cBlockingStub = CoordinationGrpc.newBlockingStub(channel);
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

    public void setCoordinatorConfig(Pair<String, Integer> coordinatorConfig) {
        this.coordinatorConfig = coordinatorConfig;
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                Participant.this.stop();
                logger.info("Keyboard Interrupt, Shutdown...");
            }
        });

        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e ){
            logger.error(e.getMessage());
        }

        initializeChannel();

        try {
            this.server = ServerBuilder.forPort(this.port)
                    .addService(new ParticipationImpl())
                    .addService(new KeyValueStoreImpl())
                    .build()
                    .start();
            logger.info("Started participant, listening on " + Integer.toString(port));
        } catch (IOException e) {
            logger.error("IO: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
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

        String configPath = "etc/server_config.json";
        ServerConfig serverConfig = Utility.readConfig(configPath);

        Participant participant = new Participant(port);
        try {
            participant.setCoordinatorConfig(new Pair<String, Integer>(
                    serverConfig.getCoordinator().getKey(),
                    serverConfig.getCoordinator().getValue()));
        } catch (Exception e) {
            logger.error("Error in setting config:" + e.getMessage());
            System.exit(1);
        }
        participant.run();
        participant.blockUntilShutdown();

    }
}