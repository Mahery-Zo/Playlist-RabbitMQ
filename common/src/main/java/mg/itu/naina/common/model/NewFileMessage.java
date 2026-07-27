package mg.itu.naina.common.model;

/**
 * Message produit par P1 (Watcher) quand un nouveau .mp3 est détecté.
 * Queue : {@link mg.itu.naina.common.Queues#NEW_FILES}.
 *
 * @param fileId       identifiant unique généré par P1
 * @param path         chemin absolu du fichier dans le répertoire
 * @param originalName nom de fichier d'origine
 * @param sizeBytes    taille en octets
 * @param detectedAt   date de détection (ISO-8601)
 */
public record NewFileMessage(
        String fileId,
        String path,
        String originalName,
        long sizeBytes,
        String detectedAt) {
}
