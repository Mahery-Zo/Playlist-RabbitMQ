package mg.itu.naina.watcher;

import io.github.cdimascio.dotenv.Dotenv;

/** Configuration de P1, lue depuis le fichier .env (avec valeurs par défaut). */
public class WatcherConfig {

    public final String repertoireDir;
    public final int scanIntervalSec;
    public final String indexFile;

    public final String rabbitHost;
    public final int rabbitPort;
    public final String rabbitUser;
    public final String rabbitPass;

    private WatcherConfig(Dotenv env) {
        this.repertoireDir   = env.get("REPERTOIRE_DIR", "./repertoire");
        this.scanIntervalSec = parseInt(env.get("SCAN_INTERVAL_SEC", "180"), 180);
        this.indexFile       = env.get("WATCHER_INDEX_FILE", "./watcher-index.txt");
        this.rabbitHost      = env.get("RABBIT_HOST", "localhost");
        this.rabbitPort      = parseInt(env.get("RABBIT_PORT", "5672"), 5672);
        this.rabbitUser      = env.get("RABBIT_USER", "guest");
        this.rabbitPass      = env.get("RABBIT_PASS", "guest");
    }

    /** Charge le .env du répertoire courant (ignoré s'il est absent). */
    public static WatcherConfig load() {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        return new WatcherConfig(env);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
