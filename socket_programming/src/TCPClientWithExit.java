import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class TCPClientWithExit {
    public static final String DEFAULT_SERVER_ADDRESS = "127.0.0.1";
    public static final int DEFAULT_SERVER_PORT = 1254;
    public static final String CLOSE_MSG = "Close socket";

    public void connectToServer(String address, int port) {
        try {
            Socket socket = new Socket(address, port);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);
            /* keep scanning input from console */
            while (true) {
                System.out.print("Please give a string: ");
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    printWriter.println(line);
                    String str;
                    if ((str = bufferedReader.readLine()) != null) {
                        /* if server send a close message,
                         * client will exit
                         */
                        if (str.equals(CLOSE_MSG)) {
                            System.err.println("Socket is closed, client exiting...");
                            break;
                        }
                        System.out.println(str);
                    }

                }
            }

            printWriter.close();
            bufferedReader.close();
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
    public static void main(String[] args) {
        int port = DEFAULT_SERVER_PORT;
        String address = DEFAULT_SERVER_ADDRESS;

        if (args.length == 0) {
            System.out.println("No address and port are given, " +
                    "using localhost and the port 1254");
        } else if (args.length == 2) {
            address = args[0];
            if (args[1].trim().matches("\\d+")) {
                port = Integer.parseInt(args[1]);
                System.out.printf("Using the port %d\n", port);
            } else {
                System.err.println("Invalid port.");
                System.exit(1);
            }
        } else {
            System.out.println("Please give two arguments");
            Scanner scanner = new Scanner(System.in);
            try {
                while (true) {
                    if (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        String[] line_arg = line.trim().split("\\s+");
                        if (line_arg.length == 2 && line_arg[1].trim().matches("\\d+")) {
                            address = line_arg[0];
                            port = Integer.parseInt(line_arg[1]);
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

        TCPClientWithExit client = new TCPClientWithExit();
        client.connectToServer(address, port);
    }
}