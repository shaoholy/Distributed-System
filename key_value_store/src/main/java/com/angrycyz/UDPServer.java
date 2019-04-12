package com.angrycyz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;

public class UDPServer implements Server{

    private static final Logger logger = LogManager.getLogger("UDPServer");

    public void closeUDPSocket(DatagramSocket s) {
        if (s != null && !s.isClosed()) {
            try {
                s.close();
            } catch (Exception e) {
                logger.error("Cannot close socket: " + e.getMessage());
            }
        }
    }

    public static String formatUDPRequest(DatagramPacket requestPacket) {
        if (requestPacket != null) {
            return String.format("Request from %s:%d: %s",
                    requestPacket.getAddress(), requestPacket.getPort(),
                    new String(requestPacket.getData()).trim());
        }
        return null;
    }

    public void startServer(HashMap<String, String> map, int port) {

        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket(port);
            byte[] buffer;

            while (true) {
                buffer = new byte[1000];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                /* print the formatted request with address and port */
                logger.debug(formatUDPRequest(request));
                String response = Utility.processRequest(new String(request.getData()).trim(), map);
                byte[] responseByt = response.getBytes();
                DatagramPacket reply = new DatagramPacket(responseByt,
                        response.length(), request.getAddress(),
                        request.getPort());
                aSocket.send(reply);
                logger.debug(map);
            }
        } catch (SocketException e) {
            logger.error("Socket: " + e.getMessage());
        } catch (IOException e) {
            logger.error("IO: " + e.getMessage());
        } finally {
            closeUDPSocket(aSocket);
        }
    }
}
