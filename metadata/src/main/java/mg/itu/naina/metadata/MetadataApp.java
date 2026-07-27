package mg.itu.naina.metadata;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;
import mg.itu.naina.common.Json;
import mg.itu.naina.common.Queues;
import mg.itu.naina.common.model.NewFileMessage;
import mg.itu.naina.common.model.SongMetadata;
import mg.itu.naina.common.model.SongMetadataMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

/**
 * P2 — Metadata.
 * Consomme {@link Queues#NEW_FILES}, extrait les métadonnées de chaque .mp3
 * et publie un {@link SongMetadataMessage} dans {@link Queues#METADATA}.
 */
public class MetadataApp {

    private static final Logger log = LoggerFactory.getLogger(MetadataApp.class);

    public static void main(String[] args) throws Exception {
        silenceJaudiotagger();

        MetadataConfig cfg = MetadataConfig.load();
        MetadataExtractor extractor = new MetadataExtractor();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(cfg.rabbitHost);
        factory.setPort(cfg.rabbitPort);
        factory.setUsername(cfg.rabbitUser);
        factory.setPassword(cfg.rabbitPass);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Déclare la topologie (idempotent) : exchange + les 2 queues utilisées.
        channel.exchangeDeclare(Queues.EXCHANGE, "direct", true);
        channel.queueDeclare(Queues.NEW_FILES, true, false, false, null);
        channel.queueBind(Queues.NEW_FILES, Queues.EXCHANGE, Queues.NEW_FILES);
        channel.queueDeclare(Queues.METADATA, true, false, false, null);
        channel.queueBind(Queues.METADATA, Queues.EXCHANGE, Queues.METADATA);

        channel.basicQos(1); // un message à la fois

        log.info("P2 Metadata démarré — broker={}:{} | en attente sur '{}'",
                cfg.rabbitHost, cfg.rabbitPort, Queues.NEW_FILES);

        DeliverCallback onMessage = (consumerTag, delivery) -> {
            long tag = delivery.getEnvelope().getDeliveryTag();
            NewFileMessage in = Json.from(delivery.getBody(), NewFileMessage.class);
            try {
                SongMetadata meta = extractor.extract(in.path());
                SongMetadataMessage out =
                        new SongMetadataMessage(in.fileId(), in.path(), in.originalName(), meta);

                channel.basicPublish(Queues.EXCHANGE, Queues.METADATA,
                        MessageProperties.PERSISTENT_TEXT_PLAIN, Json.toBytes(out));
                channel.basicAck(tag, false);

                log.info("Métadonnées extraites : {} — titre='{}', artiste='{}', genre='{}', durée={}s",
                        in.originalName(), meta.title(), meta.artist(), meta.genre(), meta.durationSec());
            } catch (Exception e) {
                // Extraction impossible (fichier absent/illisible) : on abandonne le message.
                channel.basicNack(tag, false, false);
                log.error("Échec d'extraction pour {} : {}", in.path(), e.getMessage());
            }
        };

        channel.basicConsume(Queues.NEW_FILES, false, onMessage, consumerTag -> { });

        new CountDownLatch(1).await(); // garde le programme vivant
    }

    /** jaudiotagger journalise beaucoup via java.util.logging : on le réduit. */
    private static void silenceJaudiotagger() {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(Level.SEVERE);
    }
}
