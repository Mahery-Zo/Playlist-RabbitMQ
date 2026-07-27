-- Tables utilisateurs et playlists.
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE playlists (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(512) NOT NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE playlist_songs (
    playlist_id BIGINT NOT NULL,
    song_id     BIGINT NOT NULL,
    position    INT    NOT NULL,
    PRIMARY KEY (playlist_id, position),
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id)     REFERENCES songs(id)     ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
