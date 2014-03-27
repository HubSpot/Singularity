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

CREATE TABLE taskHistory (
  taskId VARCHAR(200) PRIMARY KEY,
  requestId VARCHAR(100) NOT NULL,
  updatedAt TIMESTAMP NOT NULL DEFAULT '1971-01-01 00:00:01',
  lastTaskStatus VARCHAR(25) NULL,
  pendingType VARCHAR(25) NOT NULL,
  taskHistory BLOB NOT NULL,
  INDEX (requestId, updatedAt)
) ENGINE=InnoDB;

CREATE USER 'singularity'@'%' IDENTIFIED BY '';
GRANT ALL PRIVILEGES ON singularity.* TO 'singularity'@'%';
