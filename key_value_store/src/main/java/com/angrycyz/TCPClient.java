package com.angrycyz;

import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class TCPClient {

    private static final Logger logger = LogManager.getLogger("TCPClient");

    public final int TIME_LIMIT = 5 * 1000;

    public void connectToServer(String address, int port) {
        Socket socket = null;
        BufferedReader bufferedReader = null;
        PrintWriter printWriter = null;
        try {
            socket = new Socket(address, port);
            socket.setSoTimeout(TIME_LIMIT);
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);
            boolean timeout = false;

            /* keep scanning input from console */
            while (true) {
                /* set a limit and keep reconnect to server until success*/
                if (timeout) {
                    logger.warn("Timed out");
                    while (true) {
                        try {
                            socket = new Socket(address, port);
                            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            printWriter = new PrintWriter(socket.getOutputStream(), true);
                            logger.info("Successfully reconnected");
                            break;
                        } catch (SocketException e) {
                            /* if not connected, retrying */
                        }
                    }
                }
                logger.info("Please give an operation: ");
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    /* send to server */

                    printWriter.println(line);
                    String str;

                    long start_time = System.currentTimeMillis();
                    long end_time = start_time + TIME_LIMIT;

                    try {
                        while (System.currentTimeMillis() < end_time) {
                            if ((str = bufferedReader.readLine()) != null) {
                                logger.info(str);
                                timeout = false;
                                break;
                            }
                        }
                        if (System.currentTimeMillis() >= end_time) {
                            timeout = true;
                        }
                    } catch (SocketTimeoutException e) {
                        logger.error("Timedout: " + e.getMessage());
                        timeout = false;
                    }

                }
            }

        } catch (SocketException e) {
            logger.error("Socket: " + e.getMessage());
        } catch (IOException e) {
            logger.error("IO: " + e.getMessage());
        } finally {
            try {
            if (printWriter != null) {
                printWriter.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (socket != null) {
                socket.close();
            }
            } catch (IOException e) {
                logger.error("Cannot close socket: " + e.getMessage());
            }
        }

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

        TCPClient client = new TCPClient();
        client.connectToServer(address, port);
    }
}