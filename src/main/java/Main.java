import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server started. Listening on port 4221");
            String dir = "";

            if(args.length == 2 && args[0].equals("--directory")){
                dir = args[1];
            }

            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");

                Thread workerThread = null;

                if(!dir.isEmpty())
                    workerThread = new Thread(new Worker(clientSocket, dir));
                else
                    workerThread= new Thread(new Worker(clientSocket));
                workerThread.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
