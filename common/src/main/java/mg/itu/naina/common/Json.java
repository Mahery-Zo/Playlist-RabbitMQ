package mg.itu.naina.common;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Petit utilitaire de (dé)sérialisation JSON, partagé par les 3 programmes.
 * Les messages RabbitMQ transitent en JSON (octets UTF-8).
 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() { }

    /** Objet -> octets JSON (à publier dans la queue). */
    public static byte[] toBytes(Object value) {
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new RuntimeException("Sérialisation JSON impossible", e);
        }
    }

    /** Octets JSON (reçus de la queue) -> objet. */
    public static <T> T from(byte[] bytes, Class<T> type) {
        try {
            return MAPPER.readValue(bytes, type);
        } catch (Exception e) {
            throw new RuntimeException("Désérialisation JSON impossible", e);
        }
    }
}
