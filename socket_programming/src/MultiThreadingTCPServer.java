import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MultiThreadingTCPServer {
    public static final int DEFAULT_PORT = 1254;

    public void listenToClient(int port) {

        List<Socket> sockets = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                try {
                    for (Socket s : sockets) {
                        if (s.isClosed()) {
                            continue;
                        }
                        PrintWriter printWriter = new PrintWriter(s.getOutputStream(), true);
                        printWriter.println("Close socket");
                        printWriter.close();
                        s.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("Keyboard Interrupt, Shutdown...");
            }
        });

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(14000);
                sockets.add(socket);
                ServerTask serverTask = new ServerTask();
                serverTask.setSocket(socket);
                Thread serverThread = new Thread(serverTask);
                serverThread.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        if (args.length == 0) {
            System.out.println("No port is given, using the port 1254");
        } else if (args.length == 1) {
            if (args[0].trim().matches("\\d+")) {
                port = Integer.parseInt(args[0]);
                System.out.printf("Using the port %d\n", port);
            } else {
                System.err.println("Invalid port.");
                System.exit(1);
            }
        } else {
            System.out.println("Please give only one argument");
            Scanner scanner = new Scanner(System.in);
            try {
                while (true) {
                    if (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        String[] line_arg = line.trim().split("\\s+");
                        if (line_arg.length == 1 && args[0].trim().matches("\\d+")) {
                            port = Integer.parseInt(line_arg[0]);
                            System.out.printf("Using the port %d\n", port);
                        } else {
                            System.err.println("Invalid port");
                            System.exit(1);
                        }
                        break;
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        MultiThreadingTCPServer server = new MultiThreadingTCPServer();
        server.listenToClient(port);
    }
}