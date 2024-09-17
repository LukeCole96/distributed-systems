CREATE DATABASE cache_retry_db;

-- Switch to the newly created database
USE cache_retry_db;

CREATE TABLE db_downtime_store (
    id INT PRIMARY KEY,
    downtime_timestamp VARCHAR(255) NOT NULL
);

