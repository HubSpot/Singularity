CREATE DATABASE IF NOT EXISTS singularity CHARACTER SET = UTF8;

USE singularity;

CREATE TABLE requestHistory (
  requestName VARCHAR(100) NOT NULL,
  updatedAt TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01',
  createdAt TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01',
  request BLOB NOT NULL,
  PRIMARY KEY (requestName, updatedAt)
);

CREATE TABLE taskHistory (
  taskId VARCHAR(100) PRIMARY KEY,
  requestName VARCHAR(100) NOT NULL,
  status VARCHAR(50) NOT NULL,
  createdAt TIMESTAMP NOT NULL,
  task BLOB NOT NULL,
  INDEX (requestName)
);

CREATE TABLE taskUpdates (
  taskId VARCHAR(100) NOT NULL,
  status VARCHAR(100) NOT NULL,
  message VARCHAR(200) NULL,
  createdAt TIMESTAMP NOT NULL,
  PRIMARY KEY (taskId, status)
);
