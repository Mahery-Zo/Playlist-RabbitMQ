package mg.itu.naina.uploader;

import io.github.cdimascio.dotenv.Dotenv;

/** Configuration de P3 (RabbitMQ + URL de l'API), lue depuis le .env. */
public class UploaderConfig {

    public final String rabbitHost;
    public final int rabbitPort;
    public final String rabbitUser;
    public final String rabbitPass;
    public final String apiBaseUrl;
    public final String blacklistFile;
    public final String maxDurationFile;

    private UploaderConfig(Dotenv env) {
        this.rabbitHost = env.get("RABBIT_HOST", "localhost");
        this.rabbitPort = parseInt(env.get("RABBIT_PORT", "5672"), 5672);
        this.rabbitUser = env.get("RABBIT_USER", "guest");
        this.rabbitPass = env.get("RABBIT_PASS", "guest");
        this.apiBaseUrl = env.get("API_BASE_URL", "http://localhost:8090");
        this.blacklistFile = env.get("BLACKLIST_FILE", "./blacklist.json");
        this.maxDurationFile = env.get("MAX_DURATION_FILE", "./max_duration.txt");
    }

    public static UploaderConfig load() {
        return new UploaderConfig(Dotenv.configure().ignoreIfMissing().load());
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
