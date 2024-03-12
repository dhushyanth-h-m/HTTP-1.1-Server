import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server started. Listening on port 4221");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");

                Thread workerThread = new Thread(new Worker(clientSocket));
                workerThread.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
    class Worker implements Runnable{
      private Socket clientSocket;

      public Worker(Socket clientSocket){
          this.clientSocket = clientSocket;
      }

      public void run(){
          try{
              handleClientRequests(clientSocket);
          }catch(IOException e){
              System.out.println("IOException: "+e.getMessage());
          } finally {
              try{
                  clientSocket.close();
              }catch (IOException e){
                  System.out.println("IO Exception: "+e);
              }
          }
      }

        private static void handleClientRequests(Socket clientSocket) throws IOException {
                System.out.println("accepted new connection");
                InputStreamReader inputStream = new InputStreamReader(clientSocket.getInputStream());
                try(BufferedReader reader = new BufferedReader(inputStream)){
                    var s = reader.readLine();
                    var parts = s.split("\\s+");

                    String line;
                    StringBuilder headerBuilder = new StringBuilder();

                    // Read and append each line of the HTTP request headers
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        headerBuilder.append(line).append("\r\n");
                    }

                    String headers = headerBuilder.toString();
                    String[] headerLines = headers.split("\r\n");
                    System.out.println(Arrays.toString(headerLines));

                    if(parts.length >= 2 && "/".equals(parts[1])){
                        sendOk(clientSocket);
                    }
                    else if(parts.length >=2 && parts[1].matches("^/echo.*")){
                        String message = parts[1].substring("/echo".length());
                        message = message.replaceAll("^[/\\s]+", "");
                        sendResponse(clientSocket, message);
                    }
                    else if(parts.length >= 2 && parts[1].matches("^/user-agent.*")){
                        String message = headerLines[1].substring("User-Agent: ".length())
                                .replaceAll("^[/\\s]+", "");
                        sendHeaderResponse(clientSocket, message);
                    }
                    else{
                        send404(clientSocket);
                    }
                }
                catch (Exception e){
                    System.out.println("IO Exception"+ e);
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

        public static void sendResponse(Socket clientSocket, String message) throws IOException{
            OutputStream response = clientSocket.getOutputStream();
            String responseHeaders = "HTTP/1.1 200 OK\r\n" + // Status line
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + message.length() + "\r\n" + // Header
                    "\r\n" + // Blank line to end headers
                    message; // Response body
            response.write(responseHeaders.getBytes());
        }

        public static void sendHeaderResponse(Socket clientSocket, String message) throws IOException{
            OutputStream response = clientSocket.getOutputStream();
            String responseHeaders = "HTTP/1.1 200 OK\r\n" + // Status line
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + message.length() + "\r\n" + // Header
                    "\r\n" + // Blank line to end headers
                    message; // Response body
            response.write(responseHeaders.getBytes());
        }
  }
