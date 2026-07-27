package mg.itu.naina.watcher;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import mg.itu.naina.common.Json;
import mg.itu.naina.common.Queues;
import mg.itu.naina.common.model.NewFileMessage;

import java.io.IOException;

/**
 * Connexion RabbitMQ de P1. Déclare l'exchange et la queue NEW_FILES,
 * puis publie un message par nouveau fichier.
 */
public class RabbitPublisher implements AutoCloseable {

    private final Connection connection;
    private final Channel channel;

    public RabbitPublisher(WatcherConfig cfg) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(cfg.rabbitHost);
        factory.setPort(cfg.rabbitPort);
        factory.setUsername(cfg.rabbitUser);
        factory.setPassword(cfg.rabbitPass);

        this.connection = factory.newConnection();
        this.channel = connection.createChannel();

        // Topologie : exchange direct durable + queue durable liée par sa routing key.
        channel.exchangeDeclare(Queues.EXCHANGE, "direct", true);
        channel.queueDeclare(Queues.NEW_FILES, true, false, false, null);
        channel.queueBind(Queues.NEW_FILES, Queues.EXCHANGE, Queues.NEW_FILES);
    }

    /** Publie un message persistant (survit au redémarrage du broker). */
    public void publish(NewFileMessage message) throws IOException {
        channel.basicPublish(
                Queues.EXCHANGE,
                Queues.NEW_FILES,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                Json.toBytes(message));
    }

    @Override
    public void close() throws Exception {
        if (channel != null && channel.isOpen()) channel.close();
        if (connection != null && connection.isOpen()) connection.close();
    }
}
