import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

class Worker implements Runnable{
    private Socket clientSocket;
    String dir = "";

    public Worker(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    public Worker(Socket clientSocket, String dir){
        this.clientSocket = clientSocket;
        this.dir = dir;
    }

    public void run(){
        try{
            handleClientRequests(clientSocket, dir);
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

    private static void handleClientRequests(Socket clientSocket, String dir) throws IOException {
        System.out.println("accepted new connection");
        InputStreamReader inputStream = new InputStreamReader(clientSocket.getInputStream());
        try(BufferedReader reader = new BufferedReader(inputStream)){
            var s = reader.readLine();
            var parts = s.split("\\s+");

            String line;
            StringBuilder headerBuilder = new StringBuilder();
            StringBuilder bodyBuilder = new StringBuilder();

            int contentLength = 0;
            // Reading headers
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                headerBuilder.append(line).append("\r\n");

                // Check for Content-Length header
                if (line.startsWith("Content-Length: ")) {
                    contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
                }
            }

            // Reading the body if Content-Length is provided
            if (contentLength > 0) {
                int readChars = 0;
                char[] bodyChars = new char[contentLength];
                while (readChars < contentLength) {
                    int result = reader.read(bodyChars, readChars, contentLength - readChars);
                    if (result == -1) break; // EOF
                    readChars += result;
                }
                bodyBuilder.append(bodyChars);
            }

            String requestBody = bodyBuilder.toString();
            String headers = headerBuilder.toString();
            String[] headerLines = headers.split("\r\n");

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
            else if(parts.length >= 2 && parts[0].equals("GET") && parts[1].matches("^/files.*") && !dir.isEmpty()){
                String filePath = parts[1].substring("/files/".length());
                if(Files.exists(Path.of(dir+"/"+filePath))){
                    String fileContent = Files.readString(Path.of(dir+"/"+filePath));
                    sendFileContentResponse(clientSocket, fileContent);
                }
                else
                    send404(clientSocket);
            }
            else if(parts.length >=2 && parts[0].equals("POST")){
                String filePath = parts[1].substring("/files/".length());
                Path dirFilePath = Paths.get(dir+"/"+filePath);
                Files.writeString(dirFilePath, requestBody);
                send201Response(clientSocket);
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

    public static void sendFileContentResponse(Socket clientSocket, String message) throws IOException{
        OutputStream response = clientSocket.getOutputStream();
        String responseHeaders = "HTTP/1.1 200 OK\r\n" + // Status line
                "Content-Type: application/octet-stream\r\n" +
                "Content-Length: " + message.length() + "\r\n" + // Header
                "\r\n" + // Blank line to end headers
                message; // Response body
        response.write(responseHeaders.getBytes());
    }

    public static void send201Response(Socket clientSocket) throws IOException{
        OutputStream response = clientSocket.getOutputStream();
        String responseHeaders = "HTTP/1.1 201\r\n\r\n";
        response.write(responseHeaders.getBytes());
    }
}
