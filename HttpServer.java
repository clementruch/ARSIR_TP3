import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    private static final int PORT = 8080;
    private static final String BASE_DIR = "Site";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur démarré sur le port " + PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket);
                } catch (Exception e) {
                    System.err.println("Erreur lors du traitement de la requête : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine != null && requestLine.startsWith("GET")) {
                String filePath = requestLine.split(" ")[1];
                if (filePath.equals("/")) {
                    filePath = "/index.html";
                }

                File file = new File(BASE_DIR + filePath);
                if (file.exists() && !file.isDirectory()) {
                    byte[] fileContent = new byte[(int) file.length()];
                    try (FileInputStream fis = new FileInputStream(file)) {
                        fis.read(fileContent);
                    }

                    out.write("HTTP/1.1 200 OK\r\n".getBytes());
                    out.write("Content-Type: text/html\r\n".getBytes());
                    out.write(("Content-Length: " + fileContent.length + "\r\n".getBytes()).getBytes());
                    out.write("\r\n".getBytes());
                    out.write(fileContent);
                } else {
                    String response = "HTTP/1.1 404 Not Found\r\n\r\n<h1>404 Not Found</h1>";
                    out.write(response.getBytes());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
