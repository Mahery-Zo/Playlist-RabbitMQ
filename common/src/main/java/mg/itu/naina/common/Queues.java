package mg.itu.naina.common;

/**
 * Noms de l'exchange et des queues RabbitMQ, partagés par les 3 programmes.
 * On utilise un exchange "direct" : la routing key = le nom de la queue.
 */
public final class Queues {

    private Queues() { }

    public static final String EXCHANGE = "naina.exchange";

    /** P1 (Watcher) ->  P2 (Metadata) */
    public static final String NEW_FILES = "queue.new-files";

    /** P2 (Metadata) ->  P3 (Uploader) */
    public static final String METADATA = "queue.metadata";

    /** Messages d'upload en échec (dead-letter). */
    public static final String METADATA_DLQ = "queue.metadata.dlq";
}
