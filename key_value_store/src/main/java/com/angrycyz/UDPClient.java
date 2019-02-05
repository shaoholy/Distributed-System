package com.angrycyz;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class UDPClient {

    public final int TIME_LIMIT = 5 * 1000;
    private static final Logger logger = LogManager.getLogger("UDPClient");

    public void connectToServer(String address, int port) {
        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket();
            aSocket.setSoTimeout(TIME_LIMIT);
            InetAddress aHost = InetAddress.getByName(address);
            Scanner scanner = new Scanner(System.in);
            byte[] buffer;
            while (true) {
                logger.info("Please give an operation: ");
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    byte[] msg = line.getBytes();
                    DatagramPacket request = new DatagramPacket(msg,
                            line.length(), aHost, port);
                    aSocket.send(request);

                    buffer = new byte[1000];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    try {
                        aSocket.receive(reply);
                    } catch (SocketTimeoutException e) {
                        logger.warn("Timeout: " + e.getMessage());
                        continue;
                    }
                    String replyStr = new String(reply.getData()).trim();
                    ObjectMapper objectMapper = new ObjectMapper();
                    ServerResponse response = objectMapper.readValue(replyStr, ServerResponse.class);
                    if (response.closed) {
                        logger.error("Socket is closed, client exiting...");
                        break;
                    }
                    logger.info("Reply: " + response.response);
                }
            }
        } catch (SocketException e) {
            logger.error("Socket: " + e.getMessage());
        } catch (IOException e) {
            logger.error("IO: " + e.getMessage());
        } finally {
            if (aSocket != null) {
                aSocket.close();
            }
        }
    }

    public static void main(String args[]) {
        int port;
        String address;
        Pair<Boolean, Integer> portPair;

        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        String propLocation = "src/main/resources/log4j2.xml";
        File file = new File(propLocation);

        context.setConfigLocation(file.toURI());

        logger.info("Log properties file location: " + propLocation);

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

        UDPClient client = new UDPClient();
        client.connectToServer(address, port);
    }
}