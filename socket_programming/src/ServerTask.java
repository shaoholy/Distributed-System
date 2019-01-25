import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerTask extends Thread{
    private Socket socket;

    @Override
    public void run() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(this.socket.getOutputStream(), true);

            String inputString;
            while (true) {
                inputString = bufferedReader.readLine();
                if (inputString != null) {
                    if (inputString.equals("Exit")) {
                        break;
                    }
                    System.out.printf("From Client: %s\n", inputString);
                    String outputString = processString(inputString);
                    printWriter.println(outputString);
                }

            }
            printWriter.close();
            bufferedReader.close();
        }catch (SocketTimeoutException timeoutException) {
            try {
                PrintWriter printWriter = new PrintWriter(this.socket.getOutputStream(), true);
                printWriter.println("Close socket");
                printWriter.close();
                closeSocket(socket);
                System.err.printf("Timed out, thread %d exit\n", Thread.currentThread().getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }catch (Exception e) {
            closeSocket(socket);
            e.printStackTrace();
        }

    }

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

    public void setSocket(Socket s) {
        this.socket = s;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public static void closeSocket(Socket s) {
        if (!s.isClosed()) {
            try {
                s.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}