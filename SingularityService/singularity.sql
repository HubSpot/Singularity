CREATE DATABASE IF NOT EXISTS singularity CHARACTER SET = UTF8;

USE singularity;

CREATE TABLE requestHistory (
  requestId VARCHAR(100) NOT NULL,
  createdAt TIMESTAMP NOT NULL DEFAULT '1971-01-01 00:00:01',
  requestState VARCHAR(25) NOT NULL,
  user VARCHAR(100) NULL,
  request BLOB NOT NULL,
  PRIMARY KEY (requestId, createdAt)
) ENGINE=InnoDB;

CREATE TABLE deployHistory (
  requestId VARCHAR(100) NOT NULL,
  deployId VARCHAR(100) NOT NULL,
  createdAt TIMESTAMP NOT NULL DEFAULT '1971-01-01 00:00:01',
  user VARCHAR(100) NULL,
  deployStateAt TIMESTAMP NOT NULL DEFAULT '1971-01-01 00:00:01',
  deployState VARCHAR(25) NOT NULL,
  bytes BLOB NOT NULL,
  PRIMARY KEY (requestId, deployId),
  INDEX (requestId, createdAt)
) ENGINE=InnoDB;

CREATE TABLE taskHistory (
  taskId VARCHAR(200) PRIMARY KEY,
  requestId VARCHAR(100) NOT NULL,
  updatedAt TIMESTAMP NOT NULL DEFAULT '1971-01-01 00:00:01',
  lastTaskStatus VARCHAR(25) NULL,
  bytes BLOB NOT NULL,
  INDEX (requestId, updatedAt)
) ENGINE=InnoDB;

CREATE USER 'singularity'@'%' IDENTIFIED BY '';
GRANT ALL PRIVILEGES ON singularity.* TO 'singularity'@'%';
