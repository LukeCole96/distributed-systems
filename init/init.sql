CREATE DATABASE channel_metadata_db;

-- Switch to the newly created database
USE channel_metadata_db;

CREATE TABLE channel_metadata (
    id INT PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    metadata JSON NOT NULL,
    product VARCHAR(255) NOT NULL
);

CREATE INDEX idx_country_code ON channel_metadata(country_code);
