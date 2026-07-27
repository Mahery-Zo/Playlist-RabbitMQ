package mg.itu.naina.watcher;

import java.io.IOException;

/**
 * Version désactivée suite à la demande de retraitement en boucle.
 */
public class FileIndex {
    public FileIndex(String indexFile) throws IOException {
        // Ne fait rien
    }

    public boolean contains(String absolutePath) {
        return false; // Toujours traiter comme nouveau
    }

    public void add(String absolutePath) throws IOException {
        // Ne rien mémoriser
    }
}
