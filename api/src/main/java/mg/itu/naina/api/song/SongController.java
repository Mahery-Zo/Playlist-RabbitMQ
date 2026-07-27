package mg.itu.naina.api.song;

import mg.itu.naina.api.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final SongRepository repository;
    private final StorageService storage;

    public SongController(SongRepository repository, StorageService storage) {
        this.repository = repository;
        this.storage = storage;
    }

    /** Ingestion appelée par P3 : fichier mp3 + métadonnées JSON. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SongResponse create(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") SongMetadataInput meta) {

        String storedPath = storage.store(file);

        Song song = new Song();
        song.setTitle(meta.title());
        song.setArtist(meta.artist());
        song.setAlbum(meta.album());
        song.setGenre(meta.genre());
        song.setYear(meta.year());
        song.setDurationSec(meta.durationSec());
        song.setBitrate(meta.bitrate());
        song.setOriginalName(file.getOriginalFilename());
        song.setFilePath(storedPath);
        song.setSizeBytes(file.getSize());
        song.setCreatedAt(LocalDateTime.now());

        return SongResponse.from(repository.save(song));
    }

    /** Liste des chansons, avec recherche optionnelle. */
    @GetMapping
    public List<SongResponse> list(@RequestParam(name = "q", required = false) String q) {
        List<Song> songs;
        if (q != null && !q.isBlank()) {
            songs = repository.search(q.trim());
        } else {
            songs = repository.findAll();
        }
        return songs.stream().map(SongResponse::from).toList();
    }

    /** Détail d'une chanson. */
    @GetMapping("/{id}")
    public SongResponse getById(@PathVariable("id") Long id) {
        return repository.findById(id)
                .map(SongResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chanson introuvable"));
    }

    /** Modifier les métadonnées d'une chanson. */
    @PutMapping("/{id}")
    public SongResponse update(@PathVariable("id") Long id, @RequestBody SongUpdateInput input) {
        Song song = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chanson introuvable"));

        if (input.title() != null) song.setTitle(input.title());
        if (input.artist() != null) song.setArtist(input.artist());
        if (input.album() != null) song.setAlbum(input.album());
        if (input.genre() != null) song.setGenre(input.genre());
        if (input.year() != null) song.setYear(input.year());

        return SongResponse.from(repository.save(song));
    }

    /** Supprimer une chanson (DB + fichier disque). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        Song song = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chanson introuvable"));
        storage.delete(song.getFilePath());
        repository.delete(song);
    }

    /** Streaming audio (pour <audio> HTML). */
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable("id") Long id) {
        Song song = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chanson introuvable"));

        Resource resource = storage.load(song.getFilePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }

    /** Liste distincte des genres. */
    @GetMapping("/genres")
    public List<String> genres() {
        return repository.findDistinctGenres();
    }

    /** Liste distincte des artistes. */
    @GetMapping("/artists")
    public List<String> artists() {
        return repository.findDistinctArtists();
    }
}

