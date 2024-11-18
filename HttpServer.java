import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HttpServer {
    private static final int PORT = 8080;
    private static final String BASE_DIR = "Site";
    private static final Map<Integer, String> STATUS_MESSAGES = new HashMap<>();

    private static final Pattern REGEX_METHODE_GET = Pattern.compile("^GET\\s+/\\S*\\s+HTTP/(1\\.0|1\\.1)\\r\\nHost:\\s[^\r\n]+\\r\\n(?:[^\r\n]+\\r\\n)*\\r\\n$", Pattern.CASE_INSENSITIVE);

    static {
        STATUS_MESSAGES.put(200, "OK");
        STATUS_MESSAGES.put(400, "Bad Request");
        STATUS_MESSAGES.put(404, "Not Found");
        STATUS_MESSAGES.put(405, "Method Not Allowed");
        STATUS_MESSAGES.put(500, "Internal Server Error");
    }

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

            // Lire la requête brute
            StringBuilder rawRequest = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                rawRequest.append(line).append("\r\n");
            }
            rawRequest.append("\r\n"); // Ajouter la ligne vide
            String request = rawRequest.toString();

            System.out.println("Requête brute reçue :\n" + request);

            // Extraire la ligne de méthode (première ligne)
            String requestLine = request.split("\r\n")[0];

            // Gérer les cas où ce n'est pas un GET
            if (!requestLine.startsWith("GET ")) {
                out.write(generateResponse(405, "405 Method Not Allowed").getBytes());
                return;
            }

            Matcher matcher = REGEX_METHODE_GET.matcher(request);
            if (!matcher.matches()) {
                out.write(generateResponse(400, "400 Bad Request").getBytes());
                return;
            }

            // Extraire le chemin demandé
            String filePath = requestLine.split(" ")[1];
            if (filePath.equals("/")) {
                filePath = "/index.html";
            }

            // Récupérer et afficher le contenu demandé
            File file = new File(BASE_DIR + filePath);
            if (file.exists() && !file.isDirectory()) {
                byte[] fileContent = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(fileContent);
                }

                String header = generateResponseHeader(200, fileContent.length, "text/html");
                out.write(header.getBytes());
                out.write(fileContent);
                out.flush();
            } else {
                out.write(generateResponse(404, "<h1>404 Not Found</h1>").getBytes());
            }
        } catch (Exception e) {
            try (OutputStream out = clientSocket.getOutputStream()) {
                out.write(generateResponse(500, "<h1>500 Internal Server Error</h1>").getBytes());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static String generateResponse(int statusCode, String body) {
        int contentLength = body != null ? body.length() : 0;
        String contentType = "text/html; charset=utf-8";
        String header = generateResponseHeader(statusCode, contentLength, contentType);
        return header + (body != null ? body + "\r\n" : "");
    }

    private static String generateResponseHeader(int statusCode, int contentLength, String contentType) {
        StringBuilder header = new StringBuilder();

        String statusMessage = STATUS_MESSAGES.getOrDefault(statusCode, "Unknown Status");
        header.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));
        header.append("Date: ").append(dateFormat.format(new Date())).append("\r\n");

        header.append("Server: FisaServer\r\n");
        header.append("Connection: close\r\n");

        if (contentLength > 0) {
            header.append("Content-Length: ").append(contentLength).append("\r\n");
        }

        if (contentType != null) {
            header.append("Content-Type: ").append(contentType).append("\r\n");
        }

        header.append("\r\n");
        return header.toString();
    }
}