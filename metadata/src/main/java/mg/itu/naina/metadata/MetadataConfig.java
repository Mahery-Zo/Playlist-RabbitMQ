package mg.itu.naina.metadata;

import io.github.cdimascio.dotenv.Dotenv;

/** Configuration de P2 (connexion RabbitMQ), lue depuis le .env. */
public class MetadataConfig {

    public final String rabbitHost;
    public final int rabbitPort;
    public final String rabbitUser;
    public final String rabbitPass;

    private MetadataConfig(Dotenv env) {
        this.rabbitHost = env.get("RABBIT_HOST", "localhost");
        this.rabbitPort = parseInt(env.get("RABBIT_PORT", "5672"), 5672);
        this.rabbitUser = env.get("RABBIT_USER", "guest");
        this.rabbitPass = env.get("RABBIT_PASS", "guest");
    }

    public static MetadataConfig load() {
        return new MetadataConfig(Dotenv.configure().ignoreIfMissing().load());
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
