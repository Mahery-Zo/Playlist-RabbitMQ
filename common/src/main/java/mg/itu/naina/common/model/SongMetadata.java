package mg.itu.naina.common.model;

/**
 * Métadonnées extraites d'un .mp3 (tags ID3 + entête audio).
 * Les champs peuvent être null si absents du fichier.
 */
public record SongMetadata(
        String title,
        String artist,
        String album,
        String genre,
        Integer year,
        Integer durationSec,
        Integer bitrate) {
}
