package com.angrycyz;

import com.angrycyz.grpc.*;
import io.grpc.*;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Coordinator{
    private static final Logger logger = LogManager.getLogger("Coordinator");
    private List<Pair<String, Integer>> participantList = new ArrayList<Pair<String, Integer>>();
    private int port;
    private Server server;
    private final List<ManagedChannel> channelList = new ArrayList<ManagedChannel>();
    private final List<ParticipationGrpc.ParticipationBlockingStub> blockingStubLists
            = new ArrayList<ParticipationGrpc.ParticipationBlockingStub>();

    private ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
    private ConcurrentHashMap<String, Transaction> transactionMap = new ConcurrentHashMap<String, Transaction>();
    private String ip;
    private final int STUB_TIMEOUT = 3;

    class CoordinationImpl extends CoordinationGrpc.CoordinationImplBase {

        @Override
        public void haveCommitted(ConfirmRequest cRequest, StreamObserver<ConfirmReply> responseObserver) {
            ConfirmReply confirmReply;

            if (transactionMap.containsKey(cRequest.getTransactionId())) {
                confirmReply = ConfirmReply.newBuilder()
                        .setCommitted(true)
                        .build();
            } else {
                confirmReply = ConfirmReply.newBuilder()
                        .setCommitted(false)
                        .build();
            }

            responseObserver.onNext(confirmReply);
            responseObserver.onCompleted();
        }

        @Override
        public void getDecision(ConfirmRequest cRequest, StreamObserver<DecisionReply> responseObserver) {
            String tId = cRequest.getTransactionId();

            DecisionReply decisionReply;

            if (transactionMap.containsKey(tId)) {
                boolean decision = transactionMap.get(tId).isDecision();
                decisionReply = DecisionReply.newBuilder()
                        .setDecision(decision)
                        .build();
            } else {
                decisionReply = DecisionReply.newBuilder()
                        .setDecision(false)
                        .build();
            }
            responseObserver.onNext(decisionReply);
            responseObserver.onCompleted();

        }


    }

    class KeyValueStoreImpl extends KeyValueStoreGrpc.KeyValueStoreImplBase {
        @Override
        public void mapGet(KeyRequest kvRequest, StreamObserver<OperationReply> responseObserver) {
            String key = kvRequest.getKey();
            String value = null;
            OperationReply kvReply;
            logger.info("Get " + key);

            /* create transaction_id */
            String tId = UUID.randomUUID().toString();

            /* add transaction to transaction map */
            Transaction transaction = new Transaction(tId, key, value, Coordinator.this.ip, Coordinator.this.port);
            transactionMap.put(tId, transaction);

            if (map.containsKey(key)) {
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
            String key = kvRequest.getKey();
            String value = kvRequest.getValue();
            OperationReply kvReply;
            logger.info("Put " + key + " " + value);

            /* create transaction_id */
            String tId = UUID.randomUUID().toString();

            /* add transaction to transaction map */
            Transaction transaction = new Transaction(tId, key, value, Coordinator.this.ip, Coordinator.this.port);

            /* sends a commit message to all participants */
            boolean decision = queryToCommit("PUT", key, value, tId);

            if (decision) {
                /* all participants vote yes */
                map.put(key, value);
                kvReply = OperationReply.newBuilder()
                        .setReply("Success")
                        .build();

                transaction.setDecision(true);
                transactionMap.put(tId, transaction);

                int ack_count = commit("PUT", key, value, tId);

                /* all ack received, complete transaction */
                if (ack_count == participantList.size()) {
                    logger.info("All ack received");
                }

                responseObserver.onNext(kvReply);
                responseObserver.onCompleted();

            } else {
                /* any of the participants vote no */
                transaction.setDecision(false);
                transactionMap.put(tId, transaction);

                rollback(tId);

                /* send a failure message */
                kvReply = OperationReply.newBuilder()
                        .setReply("Failure")
                        .build();

                responseObserver.onNext(kvReply);
                responseObserver.onCompleted();
            }
        }

        @Override
        public void mapDelete(KeyRequest kvRequest, StreamObserver<OperationReply> responseObserver) {
            String key = kvRequest.getKey();
            String value = null;
            OperationReply kvReply;
            logger.info("Delete " + key);

            /* create transaction_id */
            String tId = UUID.randomUUID().toString();

            /* add transaction to transaction map */
            Transaction transaction = new Transaction(tId, key, value, Coordinator.this.ip, Coordinator.this.port);

            /* sends a commit message to all participants */
            boolean decision = queryToCommit("DELETE", key, value, tId);

            if (decision) {
                /* all participants vote yes */
                if (map.containsKey(key)) {
                    kvReply = OperationReply.newBuilder()
                            .setReply(map.remove(key))
                            .build();
                } else {
                    kvReply = OperationReply.newBuilder()
                            .setReply("Key does not exist")
                            .build();
                }

                transaction.setDecision(true);
                transactionMap.put(tId, transaction);

                /* participant commit */
                int ack_count = commit("DELETE", key, value, tId);

                /* all ack received */
                if (ack_count == participantList.size()) {
                    logger.info("All ack received");
                }

                responseObserver.onNext(kvReply);
                responseObserver.onCompleted();

            } else {
                /* any of the participants vote no */
                transaction.setDecision(false);
                transactionMap.put(tId, transaction);

                rollback(tId);

                /* send a failure message */
                kvReply = OperationReply.newBuilder()
                        .setReply("Failure")
                        .build();

                responseObserver.onNext(kvReply);
                responseObserver.onCompleted();
            }
        }

        public boolean queryToCommit(String operation, String key, String value, String tId) {
            VoteRequest voteRequest;
            if (value == null) {
                voteRequest = VoteRequest.newBuilder()
                        .setOperation(operation)
                        .setKey(key)
                        .setTransactionId(tId)
                        .build();
            } else {
                voteRequest = VoteRequest.newBuilder()
                        .setOperation(operation)
                        .setKey(key)
                        .setValue(value)
                        .setTransactionId(tId)
                        .build();
            }

            boolean decision = true;
            for (int i = 0; i < blockingStubLists.size(); i++) {
                try {
                    ParticipationGrpc.ParticipationBlockingStub blockingStub = blockingStubLists.get(i);
                    VoteReply voteReply = blockingStub
                            .withDeadlineAfter(STUB_TIMEOUT, TimeUnit.SECONDS)
                            .canCommit(voteRequest);
                    if (!voteReply.getVote()) {
                        decision = false;
                    }
                } catch (Exception e) {
                    logger.error("Erroring in receiving vote: ", e.getMessage());
                    decision = false;
                    logger.warn("channel shutdown");
                    participantList.remove(i);
                    channelList.remove(i);
                    blockingStubLists.remove(i);
                }
            }
            return decision;
        }

        public int commit(String operation, String key, String value, String tId) {
            CommitRequest commitRequest;

            if (value == null) {
                commitRequest = CommitRequest.newBuilder()
                        .setOperation(operation)
                        .setKey(key)
                        .setTransactionId(tId)
                        .build();
            } else {
                commitRequest = CommitRequest.newBuilder()
                        .setOperation(operation)
                        .setKey(key)
                        .setValue(value)
                        .setTransactionId(tId)
                        .build();
            }
            /* sends a commit message to all participants */
            int ack_count = 0;
            for (ParticipationGrpc.ParticipationBlockingStub blockingStub: blockingStubLists) {
                try {
                    AckMessage ackMessage = blockingStub
                            .withDeadlineAfter(STUB_TIMEOUT, TimeUnit.SECONDS)
                            .doCommit(commitRequest);
                    ack_count++;
                } catch (Exception e) {
                    logger.error("Erroing in receiving ack: ", e.getMessage());
                }
            }
            return ack_count;
        }

        public void rollback(String tId) {
            RollbackRequest rollbackRequest = RollbackRequest.newBuilder()
                    .setTransactionId(tId)
                    .build();
            /* sends a rollback message to all participants */
            for (ParticipationGrpc.ParticipationBlockingStub blockingStub: blockingStubLists) {
                try {
                    AckMessage ackMessage = blockingStub
                            .withDeadlineAfter(STUB_TIMEOUT, TimeUnit.SECONDS)
                            .doAbort(rollbackRequest);
                } catch (Exception e) {
                    logger.error("Erroing in receiving ack: ", e.getMessage());
                }
            }
        }
    }


    private void initializeChannels() {
        for (Pair<String, Integer> participant: Coordinator.this.participantList) {
            buildChannels(ManagedChannelBuilder
                    .forAddress(participant.getKey(), participant.getValue())
                    .usePlaintext()
                    .build());
        }
    }

    private void buildChannels(ManagedChannel channel) {
        this.channelList.add(channel);
        this.blockingStubLists.add(ParticipationGrpc.newBlockingStub(channel));
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                Coordinator.this.stop();
                logger.info("Keyboard Interrupt, Shutdown...");
            }
        });

        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e ){
            logger.error(e.getMessage());
        }

        initializeChannels();

        try {
            this.server = ServerBuilder.forPort(this.port)
                    .addService(new CoordinationImpl())
                    .addService(new KeyValueStoreImpl())
                    .build()
                    .start();
            logger.info("Started Coordinator, listening on " + Integer.toString(port));
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

    public List<Pair<String, Integer>> getParticipantList() {
        return participantList;
    }

    public void setParticipantList(List<Pair<String, Integer>> participantList) {
        this.participantList = participantList;
    }

    public void setCoordinatorConfig(int port) {
        this.port = port;
    }

    public int getCoordinatorConfig() {
        return this.port;
    }

    public static void main(String[] args) {
        /* set log configuration file path */
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        String propLocation = "src/main/resources/log4j2.xml";
        File file = new File(propLocation);

        context.setConfigLocation(file.toURI());

        logger.info("Log properties file location: " + propLocation);

        String configPath = "etc/server_config.json";
        ServerConfig serverConfig = Utility.readConfig(configPath);

        Coordinator coordinator = new Coordinator();
        try {
            coordinator.setCoordinatorConfig(serverConfig.getCoordinator().getValue());
            coordinator.setParticipantList(serverConfig.getParticipants());
        } catch (Exception e) {
            logger.error("Error in setting config: ", e.getMessage());
            System.exit(1);
        }

        coordinator.run();
        coordinator.blockUntilShutdown();
    }
}