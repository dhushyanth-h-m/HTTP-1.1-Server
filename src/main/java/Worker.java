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

            boolean isHeaderParsed = false;

            while ((line = reader.readLine()) != null) {
                if (!isHeaderParsed && line.isEmpty()) {
                    isHeaderParsed = true;
                    continue; // Skip the first blank line
                }

                if (isHeaderParsed) {
                    bodyBuilder.append(line).append("\r\n");
                } else {
                    headerBuilder.append(line).append("\r\n");
                }
            }

            String requestBody = bodyBuilder.toString();

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
            else if(parts.length >= 2 && parts[1].matches("^/files.*") && !dir.isEmpty()){
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
                Files.write(dirFilePath, requestBody.getBytes(StandardCharsets.UTF_8));
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
