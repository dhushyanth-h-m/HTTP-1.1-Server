import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

     ServerSocket serverSocket = null;
     Socket clientSocket = null;

     OutputStream response = null;

     try {
       serverSocket = new ServerSocket(4221);
       serverSocket.setReuseAddress(true);
       clientSocket = serverSocket.accept(); // Wait for connection from client.
       System.out.println("accepted new connection");

       InputStreamReader inputStream = new InputStreamReader(clientSocket.getInputStream());
       try(BufferedReader reader = new BufferedReader(inputStream)){
           var s = reader.readLine();
           var parts = s.split("\\s+");

           if(parts.length >= 2 && "/".equals(parts[1])){
                sendOk(clientSocket);
           }
           else{
               send404(clientSocket);
           }
       }
       catch (Exception e){
           System.out.println("IO Exception"+ e);
       }

     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }

    public static void sendOk(Socket clientSocket) throws IOException{
       OutputStream response = clientSocket.getOutputStream();
       String responseHeaders = "HTTP/1.1 200 OK\r\n\r\n";
       response.write(responseHeaders.getBytes());
    }

    public static void send404(Socket clientSocket) throws IOException{
        OutputStream response = clientSocket.getOutputStream();
        String responseHeaders = "HTTP/1.1 404 Not Found\r\n\r\n";
        response.write(responseHeaders.getBytes());
    }
}
