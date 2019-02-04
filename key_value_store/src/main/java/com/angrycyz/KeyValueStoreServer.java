package com.angrycyz;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class KeyValueStoreServer {

    public static final String TCP_COMM = "TCP";
    public static final String UDP_COMM = "UDP";
    private static final Logger logger = LoggerFactory.getLogger(KeyValueStoreServer.class);

    public ServerConfig parseConfig(String configPath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String configStr = new String(Files.readAllBytes(Paths.get(configPath)));
            return mapper.readValue(configStr, ServerConfig.class);
        } catch (Exception e) {
            System.err.printf(Utility.getDate() + "Cannot parse config file %s %s\n",
                    configPath, e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        int port;
        Pair<Boolean, Integer> portPair;

        if (args.length == 1 && (portPair = Utility.isPortValid(args[0])).getKey()) {
            port = portPair.getValue();
        } else {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println(Utility.getDate() + "Please give one valid port number");
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

        logger.info("Using the port %d\n", port);
        KeyValueStoreServer server = new KeyValueStoreServer();
        String configPath = "etc/comm_config.json";
        System.out.printf(Utility.getDate() + "Parse config file from %s\n", configPath);
        ServerConfig serverConfig = server.parseConfig(configPath);

        if (serverConfig.comm.equals(TCP_COMM)) {
            System.out.println(Utility.getDate() + "Starting TCP server");
            Server tcpServer = new TCPServer();
            tcpServer.startServer(port);
        } else if (serverConfig.comm.equals(UDP_COMM)) {
            System.out.println(Utility.getDate() + "Starting UDP server");
            Server udpServer = new UDPServer();
            udpServer.startServer(port);
        } else {
            System.err.printf(Utility.getDate() + "%s is not valid communication protocol," +
                    "Please check config file\n", serverConfig.comm);
            System.exit(1);
        }
    }
}