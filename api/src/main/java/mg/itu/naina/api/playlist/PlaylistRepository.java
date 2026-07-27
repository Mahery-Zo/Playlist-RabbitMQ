package mg.itu.naina.api.playlist;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUserIdOrderByCreatedAtDesc(Long userId);
}
