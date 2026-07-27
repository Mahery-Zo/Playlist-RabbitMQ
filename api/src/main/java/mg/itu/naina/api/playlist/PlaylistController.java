package mg.itu.naina.api.playlist;

import mg.itu.naina.api.auth.User;
import mg.itu.naina.api.auth.UserRepository;
import mg.itu.naina.api.song.Song;
import mg.itu.naina.api.song.SongRepository;
import mg.itu.naina.api.song.SongResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/playlists")
@Transactional
public class PlaylistController {

    private final PlaylistRepository playlistRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;

    public PlaylistController(PlaylistRepository playlistRepository,
                              SongRepository songRepository,
                              UserRepository userRepository) {
        this.playlistRepository = playlistRepository;
        this.songRepository = songRepository;
        this.userRepository = userRepository;
    }

    // --- DTOs ---

    public record GenerateRequest(
            Integer targetDurationSec,
            List<String> includeGenres,
            List<String> excludeGenres,
            List<String> includeArtists,
            List<String> excludeArtists) {}

    public record SaveRequest(String name, List<Long> songIds) {}

    public record UpdateRequest(String name, List<Long> songIds) {}

    public record MergeRequest(String name, List<Long> playlistIds) {}

    public record PlaylistResponse(
            Long id, String name, Long userId, String createdAt,
            List<SongResponse> songs, Integer totalDurationSec) {

        public static PlaylistResponse from(Playlist p) {
            List<SongResponse> songs = p.getSongs().stream()
                    .map(ps -> SongResponse.from(ps.getSong()))
                    .toList();
            int totalDuration = songs.stream()
                    .mapToInt(s -> s.durationSec() != null ? s.durationSec() : 0)
                    .sum();
            return new PlaylistResponse(
                    p.getId(), p.getName(), p.getUser().getId(),
                    p.getCreatedAt().toString(), songs, totalDuration);
        }
    }

    public record PlaylistSummary(Long id, String name, int songCount, int totalDurationSec, String createdAt) {}

    // --- Endpoints ---

    /** Génère une playlist non sauvegardée à partir de critères. */
    @PostMapping("/generate")
    public List<SongResponse> generate(@RequestBody GenerateRequest req) {
        List<String> incGenres = (req.includeGenres() == null || req.includeGenres().isEmpty()) ? null : req.includeGenres();
        List<String> excGenres = (req.excludeGenres() == null || req.excludeGenres().isEmpty()) ? null : req.excludeGenres();
        List<String> incArtists = (req.includeArtists() == null || req.includeArtists().isEmpty()) ? null : req.includeArtists();
        List<String> excArtists = (req.excludeArtists() == null || req.excludeArtists().isEmpty()) ? null : req.excludeArtists();

        List<Song> candidates = songRepository.findForPlaylist(incGenres, excGenres, incArtists, excArtists);

        // Mélanger aléatoirement
        Collections.shuffle(candidates);

        // Algorithme glouton : remplir jusqu'à targetDurationSec
        // Ajouter une marge de 59 secondes à la demande
        int baseSec = (req.targetDurationSec() != null) ? req.targetDurationSec() : 3600;
        int targetSec = baseSec + 59;
        
        List<Song> selected = new ArrayList<>();
        int currentDuration = 0;

        for (Song song : candidates) {
            int dur = (song.getDurationSec() != null) ? song.getDurationSec() : 0;
            if (currentDuration + dur <= targetSec) {
                selected.add(song);
                currentDuration += dur;
            }
        }

        // Compléter avec des chansons aléatoires si la durée cible n'est pas atteinte
        if (currentDuration < targetSec) {
            List<Song> allSongs = songRepository.findAll();
            // Filtrer celles qui ne sont pas déjà dans la sélection
            List<Song> remaining = allSongs.stream()
                    .filter(s -> !selected.contains(s))
                    .collect(Collectors.toList());
            
            Collections.shuffle(remaining);
            
            for (Song song : remaining) {
                int dur = (song.getDurationSec() != null) ? song.getDurationSec() : 0;
                if (currentDuration + dur <= targetSec) {
                    selected.add(song);
                    currentDuration += dur;
                }
            }
        }

        return selected.stream().map(SongResponse::from).toList();
    }

    /** Fusionner plusieurs playlists en une nouvelle (sans doublons). */
    @PostMapping("/merge")
    @ResponseStatus(HttpStatus.CREATED)
    public PlaylistResponse merge(@RequestBody MergeRequest req, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (req.playlistIds() == null || req.playlistIds().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il faut au moins 2 playlists à fusionner");
        }

        // Collecter toutes les chansons sans doublons, en gardant l'ordre
        java.util.LinkedHashSet<Long> uniqueSongIds = new java.util.LinkedHashSet<>();
        for (Long playlistId : req.playlistIds()) {
            Playlist p = playlistRepository.findById(playlistId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist " + playlistId + " introuvable"));
            if (!p.getUser().getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à la playlist " + playlistId);
            }
            for (PlaylistSong ps : p.getSongs()) {
                uniqueSongIds.add(ps.getSong().getId());
            }
        }

        // Créer la nouvelle playlist
        String name = req.name() != null ? req.name() : "Fusion";
        Playlist merged = new Playlist(user, name);
        merged = playlistRepository.save(merged);

        List<Song> songs = songRepository.findAllById(new ArrayList<>(uniqueSongIds));
        Map<Long, Song> songMap = songs.stream().collect(Collectors.toMap(Song::getId, s -> s));

        int i = 0;
        for (Long songId : uniqueSongIds) {
            Song song = songMap.get(songId);
            if (song != null) {
                merged.getSongs().add(new PlaylistSong(merged, song, i++));
            }
        }

        return PlaylistResponse.from(playlistRepository.save(merged));
    }

    /** Sauvegarder une playlist. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaylistResponse save(@RequestBody SaveRequest req, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        Playlist playlist = new Playlist(user, req.name());
        playlist = playlistRepository.save(playlist);

        List<Song> songs = songRepository.findAllById(req.songIds());
        Map<Long, Song> songMap = songs.stream().collect(Collectors.toMap(Song::getId, s -> s));

        for (int i = 0; i < req.songIds().size(); i++) {
            Song song = songMap.get(req.songIds().get(i));
            if (song != null) {
                playlist.getSongs().add(new PlaylistSong(playlist, song, i));
            }
        }

        return PlaylistResponse.from(playlistRepository.save(playlist));
    }

    /** Playlists de l'utilisateur connecté. */
    @GetMapping
    public List<PlaylistSummary> list(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return playlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(p -> {
                    int totalDuration = p.getSongs().stream()
                            .mapToInt(ps -> ps.getSong().getDurationSec() != null ? ps.getSong().getDurationSec() : 0)
                            .sum();
                    return new PlaylistSummary(p.getId(), p.getName(), p.getSongs().size(), totalDuration, p.getCreatedAt().toString());
                })
                .toList();
    }

    /** Détail d'une playlist. */
    @GetMapping("/{id}")
    public PlaylistResponse getById(@PathVariable("id") Long id, Authentication auth) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist introuvable"));
        Long userId = (Long) auth.getPrincipal();
        if (!playlist.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return PlaylistResponse.from(playlist);
    }

    /** Modifier la composition d'une playlist. */
    @PutMapping("/{id}")
    public PlaylistResponse update(@PathVariable("id") Long id, @RequestBody UpdateRequest req, Authentication auth) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist introuvable"));
        Long userId = (Long) auth.getPrincipal();
        if (!playlist.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        if (req.name() != null) playlist.setName(req.name());

        if (req.songIds() != null) {
            playlist.getSongs().clear();
            playlistRepository.flush();

            List<Song> songs = songRepository.findAllById(req.songIds());
            Map<Long, Song> songMap = songs.stream().collect(Collectors.toMap(Song::getId, s -> s));

            for (int i = 0; i < req.songIds().size(); i++) {
                Song song = songMap.get(req.songIds().get(i));
                if (song != null) {
                    playlist.getSongs().add(new PlaylistSong(playlist, song, i));
                }
            }
        }

        return PlaylistResponse.from(playlistRepository.save(playlist));
    }

    /** Supprimer une playlist. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id, Authentication auth) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist introuvable"));
        Long userId = (Long) auth.getPrincipal();
        if (!playlist.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        playlistRepository.delete(playlist);
    }

    /** Télécharger les mp3 d'une playlist en zip. */
    @GetMapping("/{id}/download")
    public void download(@PathVariable("id") Long id, Authentication auth, HttpServletResponse response) throws IOException {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist introuvable"));
        Long userId = (Long) auth.getPrincipal();
        if (!playlist.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + playlist.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            int index = 1;
            for (PlaylistSong ps : playlist.getSongs()) {
                Song song = ps.getSong();
                Path filePath = Path.of(song.getFilePath());
                if (Files.exists(filePath)) {
                    String entryName = String.format("%02d - %s - %s.mp3",
                            index++,
                            song.getArtist() != null ? song.getArtist() : "Unknown",
                            song.getTitle() != null ? song.getTitle() : song.getOriginalName());
                    zos.putNextEntry(new ZipEntry(entryName.replaceAll("[^a-zA-Z0-9_\\-. ]", "_")));
                    Files.copy(filePath, zos);
                    zos.closeEntry();
                }
            }
        }
    }
}
