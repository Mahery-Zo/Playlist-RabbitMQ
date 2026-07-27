package mg.itu.naina.api.song;

import java.time.LocalDateTime;

/** Vue JSON d'une chanson renvoyée par l'API (sans exposer le chemin disque interne). */
public record SongResponse(
        Long id,
        String title,
        String artist,
        String album,
        String genre,
        Integer year,
        Integer durationSec,
        Integer bitrate,
        String originalName,
        Long sizeBytes,
        LocalDateTime createdAt) {

    public static SongResponse from(Song s) {
        return new SongResponse(
                s.getId(), s.getTitle(), s.getArtist(), s.getAlbum(), s.getGenre(),
                s.getYear(), s.getDurationSec(), s.getBitrate(),
                s.getOriginalName(), s.getSizeBytes(), s.getCreatedAt());
    }
}
