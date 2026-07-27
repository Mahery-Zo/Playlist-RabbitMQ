package mg.itu.naina.common;

import mg.itu.naina.common.model.NewFileMessage;
import mg.itu.naina.common.model.SongMetadata;
import mg.itu.naina.common.model.SongMetadataMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    @Test
    void newFileMessageRoundTrip() {
        NewFileMessage msg = new NewFileMessage(
                "abc-123", "C:/repertoire/song.mp3", "song.mp3", 5_242_880L, "2026-06-10T10:00:00Z");

        NewFileMessage back = Json.from(Json.toBytes(msg), NewFileMessage.class);

        assertEquals(msg, back);
    }

    @Test
    void metadataMessageRoundTrip() {
        SongMetadata meta = new SongMetadata("Titre", "Artiste", "Album", "Jazz", 2024, 213, 320);
        SongMetadataMessage msg = new SongMetadataMessage("abc-123", "C:/repertoire/song.mp3", "song.mp3", meta);

        SongMetadataMessage back = Json.from(Json.toBytes(msg), SongMetadataMessage.class);

        assertEquals(msg, back);
    }
}
