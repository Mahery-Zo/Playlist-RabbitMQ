package mg.itu.naina.api.playlist;

import jakarta.persistence.*;
import mg.itu.naina.api.song.Song;

@Entity
@Table(name = "playlist_songs")
@IdClass(PlaylistSongId.class)
public class PlaylistSong {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @Id
    private int position;



    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    protected PlaylistSong() {}

    public PlaylistSong(Playlist playlist, Song song, int position) {
        this.playlist = playlist;
        this.song = song;
        this.position = position;
    }

    public Long getPlaylistId() { return playlist.getId(); }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public Playlist getPlaylist() { return playlist; }
    public Song getSong() { return song; }
}
