package mg.itu.naina.watcher;

import mg.itu.naina.common.Queues;
import mg.itu.naina.common.model.NewFileMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * P1 — Watcher.
 * Scanne périodiquement le répertoire, détecte les nouveaux .mp3 et publie
 * un {@link NewFileMessage} par fichier dans la queue {@link Queues#NEW_FILES}.
 */
public class WatcherApp {

    private static final Logger log = LoggerFactory.getLogger(WatcherApp.class);

    /** On ignore un fichier modifié il y a moins de N ms (copie peut-être en cours). */
    private static final long STABILITY_DELAY_MS = 3_000;

    public static void main(String[] args) throws Exception {
        WatcherConfig cfg = WatcherConfig.load();

        Path dir = Path.of(cfg.repertoireDir);
        Files.createDirectories(dir);
        FileIndex index = new FileIndex(cfg.indexFile);

        log.info("P1 Watcher démarré — répertoire={} | intervalle={}s | broker={}:{}",
                dir.toAbsolutePath(), cfg.scanIntervalSec, cfg.rabbitHost, cfg.rabbitPort);

        try (RabbitPublisher publisher = new RabbitPublisher(cfg)) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            // Premier scan immédiat, puis toutes les scanIntervalSec secondes.
            scheduler.scheduleAtFixedRate(
                    () -> scan(dir, index, publisher),
                    0, cfg.scanIntervalSec, TimeUnit.SECONDS);

            new CountDownLatch(1).await(); // garde le programme vivant (Ctrl+C pour arrêter)
        }
    }

    private static void scan(Path dir, FileIndex index, RabbitPublisher publisher) {
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(WatcherApp::isMp3)
                 .filter(Files::isRegularFile)
                 .forEach(p -> handle(p, index, publisher));
        } catch (IOException e) {
            log.error("Erreur pendant le scan de {}", dir, e);
        }
    }

    private static void handle(Path path, FileIndex index, RabbitPublisher publisher) {
        String abs = path.toAbsolutePath().toString();
        if (index.contains(abs)) {
            return; // déjà publié
        }
        try {
            long age = System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis();
            if (age < STABILITY_DELAY_MS) {
                return; // sera repris au prochain scan, une fois la copie terminée
            }

            NewFileMessage msg = new NewFileMessage(
                    UUID.randomUUID().toString(),
                    abs,
                    path.getFileName().toString(),
                    Files.size(path),
                    Instant.now().toString());

            publisher.publish(msg);
            index.add(abs);
            log.info("Nouveau mp3 publié : {} (fileId={})", msg.originalName(), msg.fileId());
        } catch (IOException e) {
            log.error("Échec de publication pour {}", abs, e);
        }
    }

    private static boolean isMp3(Path p) {
        return p.getFileName().toString().toLowerCase().endsWith(".mp3");
    }
}
