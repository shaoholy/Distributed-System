package com.team3;

import com.team3.grpc.AppServiceGrpc;
import com.team3.grpc.BalanceResponse;
import com.team3.grpc.ClientRequest;
import com.team3.grpc.LoadServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.util.*;

public class LoadBalancer {
    private static final Logger logger = LogManager.getLogger("LoadBalancer");
    private io.grpc.Server grpcServer;
    private int port;
    private ServerConfig serverConfig;
    private final int VIRTUAL_NODES_NUM = 10;
    private SortedMap<Integer, String> virtualNodes = new TreeMap<Integer, String>();
    private List<String> realNodes = new ArrayList<String>();
    private final String LOCAL_IP = "127.0.0.1";
    private List<ManagedChannel> channels = new ArrayList<ManagedChannel>();
    private List<AppServiceGrpc.AppServiceBlockingStub> stubs = new ArrayList<AppServiceGrpc.AppServiceBlockingStub>();

    LoadBalancer(int port, ServerConfig serverConfig) {
        this.port = port;
        this.serverConfig = serverConfig;

        /* add real nodes */
        for (Pair<String, Integer> appServerNode: serverConfig.appServerList) {
            String address = appServerNode.getKey().equals("localhost")? LOCAL_IP : appServerNode.getKey();
            realNodes.add(address + ":" + String.valueOf(appServerNode.getValue()));
        }
        /* add virtual nodes */
        for (String str: realNodes) {
            for (int i = 0; i < VIRTUAL_NODES_NUM; i++) {
                String virtualNodeName = str + "&&VN" + String.valueOf(i);
                int hash = getHash(virtualNodeName);
                logger.info("Added virtual node " + virtualNodeName + ", hash: " + hash);
                virtualNodes.put(hash, virtualNodeName);
            }
        }
    }

    private int getHash(String nodeName) {
        final int p = 16777619;
        int hash = (int)2166136261L;
        for (int i = 0; i < nodeName.length(); i++) {
            hash = (hash ^ nodeName.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;

        if (hash < 0) {
            hash = Math.abs(hash);
        }
        return hash;
    }

    private String getServer(String addrStr) {
        int hash = getHash(addrStr);
        SortedMap<Integer, String> subMap = virtualNodes.tailMap(hash);
        Integer i = subMap.firstKey();
        String virtualNode = subMap.get(i);
        return virtualNode.substring(0, virtualNode.indexOf("&&"));
    }

    class LoadServiceImpl extends LoadServiceGrpc.LoadServiceImplBase {
        @Override
        public void balance(ClientRequest request, StreamObserver<BalanceResponse> responseObserver) {
            String addrStr = request.getAddress() + ":" + String.valueOf(request.getPort());
            String nodeAddr = getServer(addrStr);

        }
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                LoadBalancer.this.stop();
                logger.info("Keyboard Interrupt, Shutdown...");
            }
        });
    }

    private void stop() {
        if (this.grpcServer != null) {
            this.grpcServer.shutdown();
        }
    }

    private void blockUntilShutdown() {
        try {
            if (grpcServer != null) {
                grpcServer.awaitTermination();
            }
        } catch (InterruptedException e ) {
            logger.error("Interrupted: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        /* set log configuration file location */
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        String propLocation = "src/main/resources/log4j2.xml";
        File file = new File(propLocation);

        context.setConfigLocation(file.toURI());

        logger.info("Log properties file location: " + propLocation);

        /* parse server config file */
        String configPath = "etc/server_config.json";
        ServerConfig serverConfig = Utility.readConfig(configPath);

        int port;
        Pair<Boolean, Integer> portPair;

        Scanner scanner = new Scanner(System.in);

        while (true) {
            logger.info("Please give one valid port number");
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] line_arg = line.trim().split("\\s+");
                if (line_arg.length >= 1 && (portPair = Utility.isPortValid(line_arg[0])).getKey()) {
                    port = portPair.getValue();
                    break;
                }
            }
        }


        LoadBalancer loadBalancer = new LoadBalancer(port, serverConfig);
        loadBalancer.run();
        loadBalancer.blockUntilShutdown();
    }
}