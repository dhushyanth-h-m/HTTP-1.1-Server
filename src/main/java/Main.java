import java.io.IOException;
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

       response = clientSocket.getOutputStream();
       String responseHeaders = "HTTP/1.1 200 OK\r\n\r\n";
       response.write(responseHeaders.getBytes());

     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
     finally {
         if(response != null){
             try{
                 response.close();
             }
             catch(Exception e){
                 System.out.println("IO Exception"+e);
             }
         }
     }
  }
}
