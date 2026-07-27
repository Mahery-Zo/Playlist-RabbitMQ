package mg.itu.naina.api.song;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {

    /** Liste distincte des genres (non null). */
    @Query("SELECT DISTINCT s.genre FROM Song s WHERE s.genre IS NOT NULL ORDER BY s.genre")
    List<String> findDistinctGenres();

    /** Liste distincte des artistes (non null). */
    @Query("SELECT DISTINCT s.artist FROM Song s WHERE s.artist IS NOT NULL ORDER BY s.artist")
    List<String> findDistinctArtists();

    /** Recherche par mot-clé sur titre, artiste ou album. */
    @Query("SELECT s FROM Song s WHERE " +
           "(:q IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(s.artist) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(s.album) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Song> search(@Param("q") String q);

    /** Filtrage pour la génération de playlist. */
    @Query("SELECT s FROM Song s WHERE " +
           "(:includeGenres IS NULL OR s.genre IN :includeGenres) AND " +
           "(:excludeGenres IS NULL OR s.genre NOT IN :excludeGenres) AND " +
           "(:includeArtists IS NULL OR s.artist IN :includeArtists) AND " +
           "(:excludeArtists IS NULL OR s.artist NOT IN :excludeArtists)")
    List<Song> findForPlaylist(
            @Param("includeGenres") List<String> includeGenres,
            @Param("excludeGenres") List<String> excludeGenres,
            @Param("includeArtists") List<String> includeArtists,
            @Param("excludeArtists") List<String> excludeArtists);
}

