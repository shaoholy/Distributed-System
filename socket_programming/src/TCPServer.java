import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class TCPServer {
    public static final int DEFAULT_PORT = 1254;

    private String processString(String input) {
        int left, right;
        left = 0;
        int length = input.length();
        right = length - 1;
        char[] output = new char[length];
        while (right >= left) {
            char l_char = reverseCase(input.charAt(left));
            char r_char = reverseCase(input.charAt(right));
            output[right] = l_char;
            output[left] = r_char;
            right -= 1;
            left += 1;
        }
        return String.valueOf(output);
    }

    private char reverseCase(char c) {
        int ord = (int)c;
        if (ord >= 65 && ord <= 90) {
            return (char)(ord + 32);
        } else if (ord >= 97 && ord <= 122){
            return (char)(ord - 32);
        }
        return c;
    }

    public void listenToClient(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            /* set 2 minute timeout */
            serverSocket.setSoTimeout(2 * 60 * 1000);
            Socket socket = new Socket();
            BufferedReader bufferedReader;
            PrintWriter printWriter;
            try {
                socket = serverSocket.accept();
                serverSocket.close();
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                printWriter = new PrintWriter(socket.getOutputStream(), true);

                String inputString;
                while ((inputString = bufferedReader.readLine()) != null) {
                    System.out.printf("From Client: %s\n", inputString);
                    String outputString = processString(inputString);
                    printWriter.println(outputString);
                }

                printWriter.close();
                bufferedReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket.close();

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

        TCPServer server = new TCPServer();
        server.listenToClient(port);
    }
}