package mg.itu.naina.api.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Stocke les fichiers mp3 sur le disque (dossier configurable) et renvoie leur chemin. */
@Service
public class StorageService {

    private final Path root;

    public StorageService(@Value("${naina.storage-dir}") String storageDir) {
        this.root = Path.of(storageDir).toAbsolutePath();
    }

    /** Enregistre le fichier sous un nom unique, renvoie le chemin absolu. */
    public String store(MultipartFile file) {
        try {
            Files.createDirectories(root);
            Path target = root.resolve(UUID.randomUUID() + ".mp3");
            file.transferTo(target);
            return target.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de l'enregistrement du fichier", e);
        }
    }

    /** Charge un fichier stocké en tant que Resource (pour streaming). */
    public Resource load(String filePath) {
        try {
            Path path = Path.of(filePath);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Fichier introuvable : " + filePath);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Chemin invalide : " + filePath, e);
        }
    }

    /** Supprime un fichier du disque. Ignore silencieusement si le fichier n'existe pas. */
    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de la suppression du fichier", e);
        }
    }
}
