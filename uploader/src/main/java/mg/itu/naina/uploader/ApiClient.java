package mg.itu.naina.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Envoie un fichier mp3 + ses métadonnées JSON à l'API via POST /api/songs,
 * en construisant une requête multipart/form-data à la main.
 */
public class ApiClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** Renvoie le code HTTP de la réponse (2xx = succès). */
    public int uploadSong(String filePath, String originalName, byte[] metadataJson)
            throws IOException, InterruptedException {

        byte[] fileBytes = Files.readAllBytes(Path.of(filePath));
        String boundary = "----naina-" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        // Part "file" (binaire)
        writeAscii(body, "--" + boundary + "\r\n");
        writeAscii(body, "Content-Disposition: form-data; name=\"file\"; filename=\"" + originalName + "\"\r\n");
        writeAscii(body, "Content-Type: audio/mpeg\r\n\r\n");
        body.write(fileBytes);
        writeAscii(body, "\r\n");

        // Part "metadata" (JSON)
        writeAscii(body, "--" + boundary + "\r\n");
        writeAscii(body, "Content-Disposition: form-data; name=\"metadata\"\r\n");
        writeAscii(body, "Content-Type: application/json\r\n\r\n");
        body.write(metadataJson);
        writeAscii(body, "\r\n");

        // Fin du multipart
        writeAscii(body, "--" + boundary + "--\r\n");

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/songs"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    private static void writeAscii(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
    }
}
