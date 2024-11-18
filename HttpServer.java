import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HttpServer {
    private static final int PORT = 8080;
    private static final String BASE_DIR = "Site";
    private static final Pattern REGEX_METHODE_GET = Pattern.compile("^GET\\s+/\\S*\\s+HTTP/(1\\.0|1\\.1)\\r\\nHost:\\s[^\r\n]+\\r\\n(?:[^\r\n]+\\r\\n)*\\r\\n$", Pattern.CASE_INSENSITIVE);


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
                String response = "HTTP/1.1 405 Method Not Allowed\r\nContent-Type: text/html\r\n\r\n<h1>405 Method Not Allowed</h1>";
                System.out.println("Méthode non supportée : réponse 405 envoyée.");
                out.write(response.getBytes());
                return;
            }

            Matcher matcher = REGEX_METHODE_GET.matcher(request);
            if (!matcher.matches()) {
                String response = "HTTP/1.1 400 Bad Request\r\nContent-Type: text/html\r\n\r\n<h1>400 Bad Request</h1>";
                System.out.println("Requête invalide : réponse 400 envoyée.");
                out.write(response.getBytes());
                return;
            }

            // Extraire le chemin demandé
            String filePath = requestLine.split(" ")[1];
            if (filePath.equals("/")) {
                filePath = "/index.html";
            }

            // Récupérer et afficher le contenu demandé
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
                System.out.println("Fichier introuvable, réponse 404 envoyée.");
                out.write(response.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}