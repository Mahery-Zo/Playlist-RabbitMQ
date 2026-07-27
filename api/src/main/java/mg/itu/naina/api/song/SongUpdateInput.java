package mg.itu.naina.api.song;

/** Données modifiables d'une chanson (PUT /api/songs/{id}). */
public record SongUpdateInput(
        String title,
        String artist,
        String album,
        String genre,
        Integer year) {
}
