CREATE TABLE requestHistory (
  requestId varchar(100) NOT NULL,
  createdAt timestamp(3) NOT NULL DEFAULT '1971-01-01 00:00:01.000',
  requestState varchar(25) NOT NULL,
  user varchar(100) DEFAULT NULL,
  request blob NOT NULL,
  requestJson TEXT DEFAULT NULL,
  message varchar(280) DEFAULT NULL,
  PRIMARY KEY (requestId,createdAt)
);

CREATE TABLE deployHistory (
  requestId varchar(100) NOT NULL,
  deployId varchar(100) NOT NULL,
  createdAt timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  user varchar(100) DEFAULT NULL,
  deployStateAt timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  deployState varchar(25) NOT NULL,
  bytes mediumblob NOT NULL,
  deployJson TEXT DEFAULT NULL,
  message varchar(280) DEFAULT NULL,
  PRIMARY KEY (requestId,deployId),
);

CREATE TABLE taskHistory (
  taskId varchar(200) NOT NULL,
  requestId varchar(100) NOT NULL,
  updatedAt timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  lastTaskStatus varchar(25) DEFAULT NULL,
  bytes mediumblob NOT NULL,
  taskJson TEXT DEFAULT NULL,
  runId varchar(100) DEFAULT NULL,
  deployId varchar(100) DEFAULT NULL,
  host varchar(100) CHARACTER SET ascii DEFAULT NULL,
  startedAt timestamp NULL DEFAULT NULL,
  purged tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (taskId),
);
