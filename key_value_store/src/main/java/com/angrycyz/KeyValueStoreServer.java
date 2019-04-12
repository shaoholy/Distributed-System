package com.angrycyz;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class KeyValueStoreServer {

    public static final String TCP_COMM = "TCP";
    public static final String UDP_COMM = "UDP";
    private static final Logger logger = LogManager.getLogger("KeyValueStoreServer");

    /* parse config file to get protocol name */
    public ServerConfig parseConfig(String configPath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String configStr = new String(Files.readAllBytes(Paths.get(configPath)));
            return mapper.readValue(configStr, ServerConfig.class);
        } catch (Exception e) {
            logger.error("Cannot parse config file %s %s\n",
                    configPath, e.getMessage());
        }
        return null;
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

        KeyValueStoreServer server = new KeyValueStoreServer();

        /* read the communication protocol from config file */
        String configPath = "etc/comm_config.json";
        logger.info("Parse config file from " + configPath);
        ServerConfig serverConfig = server.parseConfig(configPath);

        /* pre-populate some key-value pair to the map */
        HashMap<String, String> preMap = new HashMap<String, String>();

        int asciiA = (int)'A';

        for (int i = 0; i < 15; i++) {
            preMap.put(Character.toString((char)(asciiA + i)), Integer.toString(i));
        }

        logger.debug("Pre-populated hashmap: " + preMap);

        if (serverConfig.comm.equals(TCP_COMM)) {
            logger.info("Starting TCP server");
            Server tcpServer = new TCPServer();
            tcpServer.startServer(preMap, port);
        } else if (serverConfig.comm.equals(UDP_COMM)) {
            logger.info("Starting UDP server");
            Server udpServer = new UDPServer();
            udpServer.startServer(preMap, port);
        } else {
            logger.error(serverConfig.comm + " is not valid communication protocol," +
                    "Please check config file");
            System.exit(1);
        }
    }
}