CREATE DATABASE IF NOT EXISTS singularity CHARACTER SET = UTF8;

USE singularity;

CREATE TABLE requestHistory (
  requestId VARCHAR(100) NOT NULL,
  createdAt TIMESTAMP NOT NULL,
  requestState VARCHAR(25) NOT NULL,
  user VARCHAR(100) NULL,
  request BLOB NOT NULL,
  PRIMARY KEY (requestId, createdAt)
);

CREATE TABLE taskHistory (
  taskId VARCHAR(100) PRIMARY KEY,
  requestId VARCHAR(100) NOT NULL,
  status VARCHAR(50) NOT NULL,
  createdAt TIMESTAMP NOT NULL,
  lastTaskStatus VARCHAR(100) NULL,
  updatedAt TIMESTAMP NULL,
  task BLOB NOT NULL,
  INDEX (requestId)
);

CREATE TABLE taskUpdates (
  taskId VARCHAR(100) NOT NULL,
  status VARCHAR(100) NOT NULL,
  message VARCHAR(200) NULL,
  createdAt TIMESTAMP NOT NULL,
  PRIMARY KEY (taskId, status)
);
