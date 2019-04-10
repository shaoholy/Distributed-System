package com.angrycyz;

import com.angrycyz.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final Logger logger = LogManager.getLogger("Server");
    private List<ManagedChannel> channels = new ArrayList<ManagedChannel>();
    private List<AcceptLearnPbGrpc.AcceptLearnPbBlockingStub> aStubs
            = new ArrayList<AcceptLearnPbGrpc.AcceptLearnPbBlockingStub>();
    private io.grpc.Server server;
    private ServerConfig serverConfig;
    private List<Pair<String, Integer>> addressList = new ArrayList<Pair<String, Integer>>();
    private int port;
    private String ip;
    public Proposer proposer;
    public Acceptor acceptor;
    public Learner learner;
    private int bqSize = 10;
    public BlockingQueue<QueueMsg> proposerBq = new ArrayBlockingQueue<QueueMsg>(bqSize);
    public BlockingQueue<Boolean> proposerReplyBq = new ArrayBlockingQueue<Boolean>(bqSize);
    public BlockingQueue<QueueMsg> acceptorBq = new ArrayBlockingQueue<QueueMsg>(bqSize);
    public BlockingQueue<QueueMsg> acceptorReplyBq = new ArrayBlockingQueue<QueueMsg>(bqSize);
    public BlockingQueue<QueueMsg> learnerBq = new ArrayBlockingQueue<QueueMsg>(bqSize);
    public BlockingQueue<String> learnerReplyBq = new ArrayBlockingQueue<String>(bqSize);
    private long serverId;
    private ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();

    Server(int port, ServerConfig serverConfig) {
        this.serverId = System.currentTimeMillis();
        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        this.port = port;
        this.serverConfig = serverConfig;

        this.acceptor = new Acceptor(acceptorBq, acceptorReplyBq);
        this.proposer = new Proposer(aStubs, proposerBq, proposerReplyBq, learnerBq, serverId);
        this.learner = new Learner(learnerBq, learnerReplyBq, map);

        new Thread(acceptor).start();
        new Thread(proposer).start();
        new Thread(learner).start();
    }

    class KeyValueStoreImpl extends KeyValueStoreGrpc.KeyValueStoreImplBase {
        @Override
        public void mapDelete(KeyRequest request, StreamObserver<OperationReply> responseObserver) {
            String key = request.getKey();
            logger.info("Map: " + map);
            logger.info("Received request: " + "delete " + key);
            try {
                proposerBq.put(new QueueMsg(key, null, "delete"));
                String result = learnerReplyBq.take();
                OperationReply reply = OperationReply.newBuilder()
                        .setReply(result)
                        .build();

                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        @Override
        public void mapPut(KeyValueRequest request, StreamObserver<OperationReply> responseObserver) {
            String key = request.getKey();
            String value = request.getValue();
            logger.info("Map: " + map);
            logger.info("Received request: " + "put " + key + " " + value);
            try {
                proposerBq.put(new QueueMsg(key, value, "put"));
                String result = learnerReplyBq.take();
                OperationReply reply = OperationReply.newBuilder()
                        .setReply(result)
                        .build();

                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        @Override
        public void mapGet(KeyRequest request, StreamObserver<OperationReply> responseObserver) {
            String key = request.getKey();
            logger.info("Map: " + map);
            logger.info("Received request: " + "get " + key);
            try {
                String result;
                if (map.containsKey(key)) {
                    result = map.get(key);
                } else {
                    result = "No such key";
                }
                OperationReply reply = OperationReply.newBuilder()
                        .setReply(result)
                        .build();

                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    class AcceptLearnPbImpl extends AcceptLearnPbGrpc.AcceptLearnPbImplBase {
        @Override
        public void prepare(PaxosMsg request, StreamObserver<PaxosMsg> responseObserver) {
            String[] msg = request.getMsg().split(" ");
            String operation = msg[0];
            String key = msg[1];
            String value = null;
            if (msg.length > 2) {
                value = msg[2];
            }
            try {
                acceptorBq.put(new QueueMsg(key, value, operation, request.getPId(), request.getServerId(), 1));
                QueueMsg acceptorReply = acceptorReplyBq.take();
                String replyMsg = acceptorReply.getOperation() + " "
                        + acceptorReply.getKey() + " "
                        + acceptorReply.getValue();
                PaxosMsg reply = PaxosMsg.newBuilder()
                        .setAction("promise")
                        .setPId(acceptorReply.getpId())
                        .setServerId(acceptorReply.getServerId())
                        .setMsg(replyMsg)
                        .build();

                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

        }

        @Override
        public void accept(PaxosMsg request, StreamObserver<PaxosMsg> responseObserver) {
            String[] msg = request.getMsg().split(" ");
            String operation = msg[0];
            String key = msg[1];
            String value = null;
            if (msg.length > 2) {
                value = msg[2];
            }
            try {
                acceptorBq.put(new QueueMsg(key, value, operation, request.getPId(), request.getServerId(), 2));
                QueueMsg acceptorReply = acceptorReplyBq.take();
                String replyMsg = acceptorReply.getOperation() + " "
                        + acceptorReply.getKey() + " "
                        + acceptorReply.getValue();
                PaxosMsg reply = PaxosMsg.newBuilder()
                        .setAction("promise")
                        .setPId(acceptorReply.getpId())
                        .setServerId(acceptorReply.getServerId())
                        .setMsg(replyMsg)
                        .build();

                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        @Override
        public void learnProposal(PaxosMsg request, StreamObserver<LearnReply> responseObserver) {
            String[] msg = request.getMsg().split(" ");
            String operation = msg[0];
            String key = msg[1];
            String value = null;
            if (msg.length > 2) {
                value = msg[2];
            }

            QueueMsg finalProposal = new QueueMsg(key, value, operation, request.getPId(), request.getServerId(), -1);
            try {
                learnerBq.put(finalProposal);
                String result = learnerReplyBq.take();
                LearnReply reply = LearnReply.newBuilder()
                        .setMsg(result)
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                Server.this.stop();
                logger.info("Keyboard Interrupt, Shutdown...");
            }
        });
        try {
            this.server = ServerBuilder.forPort(this.port)
                    .addService(new KeyValueStoreImpl())
                    .addService(new AcceptLearnPbImpl())
                    .build()
                    .start();
            logger.info("Server started, listening on " + Integer.toString(port));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        initializeChannels();

    }

    private void initializeChannels() {
        for (Pair<String, Integer> processConfig: serverConfig.getProcesses()) {
            String address = processConfig.getKey().toLowerCase();
            int port = processConfig.getValue();
            if ((address.equals("localhost")
                    || address.equals("127.0.0.1")
                    || address.equals(this.ip))
                    && port == this.port) {
                continue;
            }
            buildChannels(ManagedChannelBuilder
                    .forAddress(processConfig.getKey(), processConfig.getValue())
                    .usePlaintext()
                    .build());
        }
    }

    private void buildChannels(ManagedChannel channel) {
        this.channels.add(channel);
        this.aStubs.add(AcceptLearnPbGrpc.newBlockingStub(channel));
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

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }



    public static void main(String[]  args) {
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

        /* use start epoch as server id
         * can also use argument to give server id
         */
        Server server = new Server(port, serverConfig);
        server.run();
        server.blockUntilShutdown();
    }

}