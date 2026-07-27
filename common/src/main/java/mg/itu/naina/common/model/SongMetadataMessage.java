package mg.itu.naina.common.model;

/**
 * Message produit par P2 (Metadata), consommé par P3 (Uploader).
 * Queue : {@link mg.itu.naina.common.Queues#METADATA}.
 *
 * @param fileId       identifiant repris du {@link NewFileMessage}
 * @param path         chemin absolu du fichier dans le répertoire
 * @param originalName nom de fichier d'origine
 * @param metadata     métadonnées extraites
 */
public record SongMetadataMessage(
        String fileId,
        String path,
        String originalName,
        SongMetadata metadata) {
}
