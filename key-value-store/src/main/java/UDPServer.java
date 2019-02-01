import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UDPServer implements Server{

    public void closeUDPSocket(DatagramSocket s) {
        if (s != null && !s.isClosed()) {
            try {
                s.close();
            } catch (Exception e) {
                System.err.println(Utility.getDate() + "Cannot close socket: " + e.getMessage());
            }
        }
    }

    public static String formatUDPRequest(DatagramPacket requestPacket) {
        if (requestPacket != null) {
            String requestString = new String(requestPacket.getData());
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                ClientRequest request = objectMapper.readValue(requestString, ClientRequest.class);
                return String.format("Request from %s:%d: %s %s %s\n",
                        requestPacket.getAddress(), requestPacket.getPort(),
                        request.method, request.key, request.value);
            } catch (Exception e) {
                System.err.println(Utility.getDate() + "Cannot format request: " + e.getMessage());
            }
        }
        return null;
    }

    public void startServer(int port) {
        HashMap<String, String> map = new HashMap<String, String>();

        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket(port);
            byte[] buffer = new byte[1000];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                System.out.printf(formatUDPRequest(request));
                String output = Utility.processRequest(new String(request.getData()), map);
                String response = Utility.createResponse(false, output);
                byte[] responseByt = response.getBytes();
                DatagramPacket reply = new DatagramPacket(responseByt,
                        response.length(), request.getAddress(),
                        request.getPort());
                aSocket.send(reply);
            }
        } catch (SocketException e) {
            System.out.println(Utility.getDate() + "Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println(Utility.getDate() + "IO: " + e.getMessage());
        } finally {
            closeUDPSocket(aSocket);
        }
    }
}
