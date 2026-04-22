-- liquibase formatted sql

-- changeset Mofurka:1776796155734-1
CREATE TABLE players
(
    id         UUID                        NOT NULL,
    username   VARCHAR(64)                 NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_players PRIMARY KEY (id)
);

-- changeset Mofurka:1776796155734-2
ALTER TABLE players
    ADD CONSTRAINT uc_players_username UNIQUE (username);

