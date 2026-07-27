package mg.itu.naina.metadata;

import mg.itu.naina.common.model.SongMetadata;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;

/** Extrait les tags ID3 + l'entête audio d'un .mp3 avec jaudiotagger. */
public class MetadataExtractor {

    public SongMetadata extract(String path) throws Exception {
        AudioFile audio = AudioFileIO.read(new File(path));
        Tag tag = audio.getTag();              // peut être null si aucun tag
        AudioHeader header = audio.getAudioHeader();

        return new SongMetadata(
                field(tag, FieldKey.TITLE),
                field(tag, FieldKey.ARTIST),
                field(tag, FieldKey.ALBUM),
                field(tag, FieldKey.GENRE),
                parseYear(field(tag, FieldKey.YEAR)),
                header.getTrackLength(),                 // durée en secondes
                (int) header.getBitRateAsNumber());      // débit en kbps
    }

    /** Renvoie la valeur du tag, ou null si absente/vide. */
    private String field(Tag tag, FieldKey key) {
        if (tag == null) {
            return null;
        }
        String value = tag.getFirst(key);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** Extrait l'année (4 chiffres) d'une chaîne de date éventuellement complète. */
    private Integer parseYear(String raw) {
        if (raw == null || raw.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(raw.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
