-- Task 6 - Campus Event Registration (MySQL 8 / MariaDB 10)
CREATE DATABASE IF NOT EXISTS campus;
USE campus;

DROP TABLE IF EXISTS registrations;
DROP TABLE IF EXISTS events;

CREATE TABLE events (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(80)  NOT NULL UNIQUE,
    venue       VARCHAR(80)  NOT NULL,
    event_date  DATE         NOT NULL,
    capacity    INT          NOT NULL CHECK (capacity > 0)
);

CREATE TABLE registrations (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    reg_no        CHAR(9)      NOT NULL,
    student_name  VARCHAR(80)  NOT NULL,
    email         VARCHAR(120) NOT NULL,
    department    VARCHAR(40)  NOT NULL,
    event_id      INT          NOT NULL,
    registered_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT uq_student_event UNIQUE (reg_no, event_id),   -- a student cannot register twice for one event
    INDEX idx_event_id (event_id, id)                         -- filter by event + page by id
);

INSERT INTO events (name, venue, event_date, capacity) VALUES
  ('Dance Battle',      'Open Air Theatre', '2026-12-05', 200),
  ('Quiz Mania',        'Seminar Hall 1',   '2026-12-05', 120),
  ('Code Sprint',       'Lab Block 3',      '2026-12-06', 150),
  ('Battle of Bands',   'Main Auditorium',  '2026-12-06', 300),
  ('Short Film Contest','Mini Theatre',     '2026-12-07',  80);

-- Application user (change the password for real use)
-- CREATE USER 'student'@'localhost' IDENTIFIED BY 'student123';
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP ON campus.* TO 'student'@'localhost';
