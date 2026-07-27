package mg.itu.naina.api.song;

/**
 * Métadonnées reçues dans la part JSON "metadata" du POST /api/songs.
 * Correspond (par contrat) au SongMetadata produit par P2.
 */
public record SongMetadataInput(
        String title,
        String artist,
        String album,
        String genre,
        Integer year,
        Integer durationSec,
        Integer bitrate) {
}
