-- Table des chansons ingérées par le pipeline (P3 -> API).
CREATE TABLE songs (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    title         VARCHAR(512),
    artist        VARCHAR(512),
    album         VARCHAR(512),
    genre         VARCHAR(255),
    release_year  INT,
    duration_sec  INT,
    bitrate       INT,
    original_name VARCHAR(512) NOT NULL,
    file_path     VARCHAR(1024) NOT NULL,
    size_bytes    BIGINT,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_songs_genre  ON songs (genre);
CREATE INDEX idx_songs_artist ON songs (artist);
