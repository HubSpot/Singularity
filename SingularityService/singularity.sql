CREATE DATABASE IF NOT EXISTS singularity CHARACTER SET = UTF8;

USE singularity;

CREATE TABLE requestHistory (
  requestId VARCHAR(100) NOT NULL,
  createdAt TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01',
  requestState VARCHAR(25) NOT NULL,
  user VARCHAR(100) NULL,
  request BLOB NOT NULL,
  PRIMARY KEY (requestId, createdAt)
) ENGINE=InnoDB;

CREATE TABLE taskHistory (
  taskId VARCHAR(100) PRIMARY KEY,
  requestId VARCHAR(100) NOT NULL,
  status VARCHAR(50) NOT NULL,
  createdAt TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01',
  lastTaskStatus VARCHAR(100) NULL,
  updatedAt TIMESTAMP NULL,
  directory VARCHAR(500) NULL,
  task BLOB NOT NULL,
  INDEX (requestId, createdAt),
  INDEX (requestId, updatedAt),
  INDEX (requestId, lastTaskStatus)
) ENGINE=InnoDB;
 
CREATE TABLE taskUpdates (
  id INT NOT NULL AUTO_INCREMENT,
  taskId VARCHAR(100) NOT NULL,
  status VARCHAR(100) NOT NULL,
  message VARCHAR(200) NULL,
  createdAt TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01',
  INDEX (taskId),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE USER 'singularity'@'%' IDENTIFIED BY '';
GRANT ALL PRIVILEGES ON singularity.* TO 'singularity'@'%';
