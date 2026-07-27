package mg.itu.naina.api.admin;

import mg.itu.naina.api.playlist.PlaylistRepository;
import mg.itu.naina.api.song.Song;
import mg.itu.naina.api.song.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Administration — permet de remettre à zéro toutes les données
 * (chansons en base, fichiers stockés, playlists, watcher-index).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;

    @Value("${naina.storage-dir}")
    private String storageDir;

    @Value("${naina.watcher-index:./watcher-index.txt}")
    private String watcherIndexFile;

    @Value("${naina.repertoire-dir:./repertoire}")
    private String repertoireDir;

    public AdminController(SongRepository songRepository,
                           PlaylistRepository playlistRepository) {
        this.songRepository = songRepository;
        this.playlistRepository = playlistRepository;
    }

    public record ResetResult(int deletedSongs, int deletedFiles, int deletedPlaylists, String message) {}

    /** Supprime TOUT : playlists, chansons en base, fichiers stockés, répertoire source, et watcher-index. */
    @DeleteMapping("/reset")
    @Transactional
    public ResetResult resetAll() {
        // 1. Supprimer toutes les playlists (et leurs playlist_songs via cascade)
        int playlistCount = (int) playlistRepository.count();
        playlistRepository.deleteAll();
        log.info("RESET — {} playlists supprimées", playlistCount);

        // 2. Supprimer les fichiers stockés sur disque pour chaque chanson
        List<Song> allSongs = songRepository.findAll();
        int fileCount = 0;
        for (Song song : allSongs) {
            try {
                Path filePath = Path.of(song.getFilePath());
                if (Files.deleteIfExists(filePath)) {
                    fileCount++;
                }
            } catch (IOException e) {
                log.warn("Impossible de supprimer le fichier : {}", song.getFilePath(), e);
            }
        }
        log.info("RESET — {} fichiers stockés supprimés", fileCount);

        // 3. Supprimer toutes les chansons de la base
        int songCount = allSongs.size();
        songRepository.deleteAll();
        log.info("RESET — {} chansons supprimées de la base", songCount);

        // 4. Vider le dossier de stockage (au cas où il resterait des fichiers orphelins)
        cleanDirectory(Path.of(storageDir));

        // 5. Vider le répertoire source (là où l'utilisateur dépose ses MP3)
        cleanDirectory(Path.of(repertoireDir));
        log.info("RESET — répertoire source vidé : {}", repertoireDir);

        // 6. Supprimer le fichier watcher-index.txt
        try {
            Files.deleteIfExists(Path.of(watcherIndexFile));
            log.info("RESET — watcher-index.txt supprimé");
        } catch (IOException e) {
            log.warn("Impossible de supprimer watcher-index.txt : {}", e.getMessage());
        }

        return new ResetResult(songCount, fileCount, playlistCount,
                "Remise à zéro complète effectuée avec succès.");
    }

    /** Supprime tous les fichiers dans un dossier (sans supprimer le dossier lui-même). */
    private void cleanDirectory(Path dir) {
        if (!Files.isDirectory(dir)) return;
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Erreur lors du nettoyage du dossier {} : {}", dir, e.getMessage());
        }
    }
}
