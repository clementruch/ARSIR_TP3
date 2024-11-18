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

            // Boucle "infini" pour accepter une connexion
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Nouvelle connexion : " + clientSocket.getInetAddress());
                    handleRequest(clientSocket);
                } catch (Exception e) {
                    System.err.println("Erreur : " + e.getMessage());
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
                System.out.println("Requested file: " + file.getAbsolutePath());
                if (file.exists() && !file.isDirectory()) {
                    byte[] fileContent = new byte[(int) file.length()];
                    try (FileInputStream fis = new FileInputStream(file)) {
                        fis.read(fileContent);
                    }

                    String contentType = "text/html";
                    if (filePath.endsWith(".css")) {
                        contentType = "text/css";
                    } else if (filePath.endsWith(".ico")) {
                        contentType = "image/x-icon";
                    }

                    out.write("HTTP/1.1 200 OK\r\n".getBytes());
                    out.write(("Content-Type: " + contentType + "\r\n").getBytes());
                    out.write(("Content-Length: " + fileContent.length + "\r\n").getBytes());
                    out.write("\r\n".getBytes());

                    out.write(fileContent);
                    out.flush();
                } else {
                    String response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/html\r\n\r\n<h1>404 Not Found</h1>";
                    System.out.println("File not found, delivering 404 response");
                    out.write(response.getBytes());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
