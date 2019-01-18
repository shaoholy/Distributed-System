import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TCPClient {
    public static final String DEFAULT_SERVER_ADDRESS = "127.0.0.1";
    public static final int DEFAULT_SERVER_PORT = 1254;

    public void connectToServer(String address, int port) {
        try {
            Socket socket = new Socket(address, port);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);
            while (true) {
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    printWriter.println(line);
                    break;
                }
            }

            String str;
            if ((str = bufferedReader.readLine()) != null) {
                System.out.println(str);
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
                port = Integer.parseInt(args[0]);
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
                        if (line_arg.length == 2 && args[1].trim().matches("\\d+")) {
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

        TCPClient client = new TCPClient();
        client.connectToServer(address, port);
    }
}