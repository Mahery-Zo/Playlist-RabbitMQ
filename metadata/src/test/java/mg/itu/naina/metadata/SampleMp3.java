package mg.itu.naina.metadata;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Génère un vrai .mp3 minimal mais valide (frames MPEG1 Layer III silencieuses)
 * et y pose des tags ID3v2.3. Sert aux tests et à produire un fichier de démo.
 *
 * Usage CLI :  java -cp <test-classes>;<metadata.jar> mg.itu.naina.metadata.SampleMp3 [chemin]
 */
public final class SampleMp3 {

    private SampleMp3() { }

    public static void write(Path target, String title, String artist,
                             String album, String genre, String year) throws Exception {
        // 1) Frames MPEG1 Layer III, 128 kbps, 44.1 kHz -> 417 octets/frame. 80 frames ~ 2 s.
        int frames = 80;
        int frameSize = 417;
        byte[] data = new byte[frames * frameSize];
        for (int i = 0; i < frames; i++) {
            int off = i * frameSize;
            data[off]     = (byte) 0xFF;   // sync
            data[off + 1] = (byte) 0xFB;   // MPEG1, Layer III, pas de CRC
            data[off + 2] = (byte) 0x90;   // 128 kbps, 44.1 kHz
            data[off + 3] = (byte) 0x00;   // stéréo, reste à 0 (silence)
        }
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.write(target, data);

        // 2) Tags ID3v2.3 posés par jaudiotagger.
        AudioFile audio = AudioFileIO.read(target.toFile());
        Tag tag = new ID3v23Tag();
        tag.setField(FieldKey.TITLE, title);
        tag.setField(FieldKey.ARTIST, artist);
        tag.setField(FieldKey.ALBUM, album);
        tag.setField(FieldKey.GENRE, genre);
        tag.setField(FieldKey.YEAR, year);
        audio.setTag(tag);
        AudioFileIO.write(audio);
    }

    public static void main(String[] args) throws Exception {
        Path target = Path.of(args.length > 0 ? args[0] : "repertoire/demo.mp3");
        write(target, "Ma Chanson Demo", "Naina", "Album Demo", "Jazz", "2024");
        System.out.println("Écrit : " + target.toAbsolutePath());
    }
}
