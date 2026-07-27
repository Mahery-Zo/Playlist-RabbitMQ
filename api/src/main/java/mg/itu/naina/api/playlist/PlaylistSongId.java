package mg.itu.naina.api.playlist;

import java.io.Serializable;
import java.util.Objects;

/** Clé composite pour PlaylistSong (playlist_id + position). */
public class PlaylistSongId implements Serializable {
    private Long playlist;
    private int position;

    public PlaylistSongId() {}

    public PlaylistSongId(Long playlist, int position) {
        this.playlist = playlist;
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaylistSongId that = (PlaylistSongId) o;
        return position == that.position && Objects.equals(playlist, that.playlist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playlist, position);
    }
}
