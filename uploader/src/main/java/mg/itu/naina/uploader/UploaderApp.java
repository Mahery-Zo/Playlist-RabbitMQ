package mg.itu.naina.uploader;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;
import mg.itu.naina.common.Json;
import mg.itu.naina.common.Queues;
import mg.itu.naina.common.model.SongMetadataMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * P3 — Uploader.
 * Consomme {@link Queues#METADATA}, envoie chaque fichier + métadonnées à
 * l'API,
 * et en cas de succès supprime le fichier du répertoire. Les échecs partent en
 * DLQ.
 */
public class UploaderApp {

    private static final Logger log = LoggerFactory.getLogger(UploaderApp.class);

    public static void main(String[] args) throws Exception {
        UploaderConfig cfg = UploaderConfig.load();
        ApiClient api = new ApiClient(cfg.apiBaseUrl);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(cfg.rabbitHost);
        factory.setPort(cfg.rabbitPort);
        factory.setUsername(cfg.rabbitUser);
        factory.setPassword(cfg.rabbitPass);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Topologie (idempotent) : exchange + queue source + queue DLQ.
        channel.exchangeDeclare(Queues.EXCHANGE, "direct", true);
        channel.queueDeclare(Queues.METADATA, true, false, false, null);
        channel.queueBind(Queues.METADATA, Queues.EXCHANGE, Queues.METADATA);
        channel.queueDeclare(Queues.METADATA_DLQ, true, false, false, null);
        channel.queueBind(Queues.METADATA_DLQ, Queues.EXCHANGE, Queues.METADATA_DLQ);

        channel.basicQos(1);

        log.info("P3 Uploader démarré — API={} | en attente sur '{}'",
                cfg.apiBaseUrl, Queues.METADATA);

        DeliverCallback onMessage = (consumerTag, delivery) -> {
            long tag = delivery.getEnvelope().getDeliveryTag();
            SongMetadataMessage in = Json.from(delivery.getBody(), SongMetadataMessage.class);
            Path file = Path.of(in.path());
            
            // --- Rechargement à la volée de la blacklist et durée max ---
            BlacklistData blacklist = new BlacklistData();
            Path blacklistPath = Path.of(cfg.blacklistFile);
            if (Files.exists(blacklistPath)) {
                try {
                    blacklist = mg.itu.naina.common.Json.from(Files.readAllBytes(blacklistPath), BlacklistData.class);
                } catch(Exception ignored) {}
            }
            
            int maxDurationSec = -1;
            Path maxDurationPath = Path.of(cfg.maxDurationFile);
            if (Files.exists(maxDurationPath)) {
                try {
                    maxDurationSec = Integer.parseInt(Files.readString(maxDurationPath).trim());
                } catch(Exception ignored) {}
            }
            // -------------------------------------------------------------
            
            // --- AJOUT C : Filtrage via la blacklist ---
            String artist = in.metadata().artist();
            String genre = in.metadata().genre();
            
            boolean isBlacklisted = false;
            if (artist != null && blacklist.artists != null && blacklist.artists.stream().anyMatch(artist::equalsIgnoreCase)) {
                isBlacklisted = true;
            }
            if (genre != null && blacklist.genres != null && blacklist.genres.stream().anyMatch(genre::equalsIgnoreCase)) {
                isBlacklisted = true;
            }

            if (isBlacklisted) {
                log.warn("Fichier blacklisté ignoré (conservé dans le répertoire) : {} (Artiste: {}, Genre: {})", in.originalName(), artist, genre);
                channel.basicAck(tag, false); // On acquitte le message pour le retirer de la queue
                return;
            }
            // -------------------------------------------

            // --- Filtrage par durée maximale ---
            Integer durationSec = in.metadata().durationSec();
            if (maxDurationSec > 0 && durationSec != null && durationSec > maxDurationSec) {
                log.warn("Fichier trop long ignoré (conservé dans le répertoire) : {} (Durée: {}s, Max: {}s)",
                        in.originalName(), durationSec, maxDurationSec);
                channel.basicAck(tag, false);
                return;
            }
            // -------------------------------------------
 
            if (!Files.exists(file)) {
                log.warn("Fichier introuvable, message abandonné : {}", in.path());
                channel.basicAck(tag, false);
                return;
            }

            try {
                int status = api.uploadSong(in.path(), in.originalName(), Json.toBytes(in.metadata()));
                if (status >= 200 && status < 300) {
                    Files.deleteIfExists(file);
                    channel.basicAck(tag, false);
                    log.info("Uploadé puis supprimé du répertoire : {} (HTTP {})", in.originalName(), status);
                } else {
                    toDeadLetter(channel, delivery.getBody());
                    channel.basicAck(tag, false);
                    log.error("API a refusé {} (HTTP {}) — envoyé en DLQ, fichier conservé", in.originalName(), status);
                }
            } catch (Exception e) {
                toDeadLetter(channel, delivery.getBody());
                channel.basicAck(tag, false);
                log.error("Échec d'envoi de {} — envoyé en DLQ, fichier conservé : {}", in.path(), e.getMessage());
            }
        };

        channel.basicConsume(Queues.METADATA, false, onMessage, consumerTag -> {
        });

        new CountDownLatch(1).await(); // garde le programme vivant
    }

    private static void toDeadLetter(Channel channel, byte[] body) throws java.io.IOException {
        channel.basicPublish(Queues.EXCHANGE, Queues.METADATA_DLQ,
                MessageProperties.PERSISTENT_TEXT_PLAIN, body);
    }

    public static class BlacklistData {
        public java.util.List<String> artists;
        public java.util.List<String> genres;
    }
}
