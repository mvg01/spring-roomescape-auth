DROP TABLE IF EXISTS reservation_waiting;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS theme;
DROP TABLE IF EXISTS reservation_time;
DROP TABLE IF EXISTS member;

CREATE TABLE member
(
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(255) NOT NULL,
    name     VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL,
    UNIQUE(login_id),
    PRIMARY KEY (id)
);

CREATE TABLE reservation_time
(
    id        BIGINT NOT NULL AUTO_INCREMENT,
    start_at  TIME   NOT NULL,
    finish_at TIME   NOT NULL,
    UNIQUE(start_at, finish_at),
    PRIMARY KEY (id)
);

CREATE TABLE theme
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    image_url   VARCHAR(255) NOT NULL,
    UNIQUE(name),
    PRIMARY KEY (id)
);

CREATE TABLE reservation
(
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    member_id BIGINT       NOT NULL,
    date      DATE         NOT NULL,
    time_id   BIGINT,
    theme_id  BIGINT,
    UNIQUE(date, time_id, theme_id),
    PRIMARY KEY (id),
    FOREIGN KEY (member_id) REFERENCES member (id),
    FOREIGN KEY (time_id) REFERENCES reservation_time (id),
    FOREIGN KEY (theme_id) REFERENCES theme (id)
);

CREATE TABLE reservation_waiting
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    member_id  BIGINT       NOT NULL,
    date       DATE         NOT NULL,
    time_id    BIGINT,
    theme_id   BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(member_id, date, time_id, theme_id),
    PRIMARY KEY (id),
    FOREIGN KEY (member_id) REFERENCES member (id),
    FOREIGN KEY (time_id) REFERENCES reservation_time (id),
    FOREIGN KEY (theme_id) REFERENCES theme (id)
);
