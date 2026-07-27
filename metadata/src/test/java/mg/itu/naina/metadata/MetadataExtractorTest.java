package mg.itu.naina.metadata;

import mg.itu.naina.common.model.SongMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataExtractorTest {

    @Test
    void extractsId3Tags(@TempDir Path dir) throws Exception {
        Path mp3 = dir.resolve("song.mp3");
        SampleMp3.write(mp3, "Titre Test", "Artiste Test", "Album Test", "Jazz", "2024");

        SongMetadata m = new MetadataExtractor().extract(mp3.toString());

        assertEquals("Titre Test", m.title());
        assertEquals("Artiste Test", m.artist());
        assertEquals("Album Test", m.album());
        assertNotNull(m.genre());
        assertEquals(2024, m.year());
        assertEquals(128, m.bitrate());
        assertTrue(m.durationSec() >= 1, "durée attendue > 0, obtenue=" + m.durationSec());
    }
}
