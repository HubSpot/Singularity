--liquibase formatted sql

--changeset mbell:1 dbms:postgresql
CREATE TABLE taskHistory (
  taskId varchar(200) NOT NULL DEFAULT '',
  requestId varchar(100) NOT NULL,
  lastTaskStatus varchar(25) DEFAULT NULL,
  updatedAt TIMESTAMP NOT NULL DEFAULT '1971-01-01 00:00:01',
  bytes bytea NOT NULL,
  runId VARCHAR(100) NULL,
  deployId VARCHAR(100) NULL,
  host VARCHAR(100) NULL,
  startedAt TIMESTAMP NULL,
  purged boolean not null default false,
  PRIMARY KEY (taskId)
);
CREATE INDEX idx_task_requestId_2 ON taskHistory (requestId,updatedAt);
CREATE INDEX idx_requestId_3 ON taskHistory (requestId,lastTaskStatus);
CREATE INDEX idx_task_deploy ON taskHistory (requestId, deployId, startedAt);
CREATE INDEX idx_task_run ON taskHistory(runId, requestId);
CREATE INDEX idx_task_st ON taskHistory(requestId, startedAt);
CREATE INDEX idx_lastStatus ON taskHistory(requestId, lastTaskStatus, startedAt);
CREATE INDEX idx_host ON taskHistory(requestId, host, startedAt);
CREATE INDEX idx_updated ON taskHistory(updatedAt, requestId);
CREATE INDEX idx_purged ON taskHistory(requestId, purged, updatedAt);
CREATE INDEX idx_task_stt ON taskHistory(startedAt);

CREATE TABLE requestHistory (
  requestId varchar(100) NOT NULL,
  createdAt timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  requestState varchar(25) NOT NULL,
  f_user varchar(100) DEFAULT NULL,
  request bytea NOT NULL,
  message VARCHAR(280) NULL,
  PRIMARY KEY (requestId,createdAt)
);


CREATE TABLE deployHistory (
  requestId varchar(100) NOT NULL,
  deployId varchar(100) NOT NULL,
  createdAt timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  f_user varchar(100) DEFAULT NULL,
  deployStateAt timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  deployState varchar(25) NOT NULL,
  bytes bytea NOT NULL,
  message VARCHAR(280) NULL,
  PRIMARY KEY (requestId,deployId)
);
CREATE INDEX idx_deploy_request ON deployHistory(requestId,createdAt);