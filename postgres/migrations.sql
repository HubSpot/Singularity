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

--changeset ssalinas:2 dbms:postgresql
ALTER TABLE taskUsage CHARACTER SET ascii COLLATE ascii_bin ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8;
ALTER TABLE taskUsage
  MODIFY COLUMN requestId varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '',
  MODIFY COLUMN taskId varchar(200) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '';

--changeset ssalinas:3 dbms:postgresql
ALTER TABLE taskHistory CHARACTER SET ascii COLLATE ascii_bin;
ALTER TABLE taskHistory
  MODIFY COLUMN taskId varchar(200) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN requestId varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN lastTaskStatus ENUM ('TASK_LAUNCHED', 'TASK_STAGING', 'TASK_STARTING', 'TASK_RUNNING', 'TASK_CLEANING', 'TASK_KILLING', 'TASK_FINISHED', 'TASK_FAILED', 'TASK_KILLED', 'TASK_LOST', 'TASK_LOST_WHILE_DOWN', 'TASK_ERROR', 'TASK_DROPPED', 'TASK_GONE', 'TASK_UNREACHABLE', 'TASK_GONE_BY_OPERATOR', 'TASK_UNKNOWN') NOT NULL,
  MODIFY COLUMN runId varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  MODIFY COLUMN deployId varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  ADD COLUMN `taskJson` JSON DEFAULT NULL,
  ADD KEY `requestDeployUpdated` (requestId, deployId, updatedAt),
  ADD KEY `hostUpdated` (host, updatedAt);

--changeset ssalinas:4 dbms:postgresql
ALTER TABLE requestHistory CHARACTER SET ascii COLLATE ascii_bin;
ALTER TABLE requestHistory
  MODIFY COLUMN requestId varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN requestState ENUM ('CREATED', 'UPDATED', 'DELETING', 'DELETED', 'PAUSED', 'UNPAUSED', 'ENTERED_COOLDOWN', 'EXITED_COOLDOWN', 'FINISHED', 'DEPLOYED_TO_UNPAUSE', 'BOUNCED', 'SCALED', 'SCALE_REVERTED') NOT NULL,
  MODIFY COLUMN user varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  MODIFY COLUMN message varchar(280) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL
  ADD COLUMN `requestJson` JSON DEFAULT NULL;

--changeset ssalinas:5 dbms:postgresql
ALTER TABLE deployHistory CHARACTER SET ascii COLLATE ascii_bin;
ALTER TABLE deployHistory
  MODIFY COLUMN requestId varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN deployId varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN user varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  MODIFY COLUMN message varchar(280) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  MODIFY COLUMN deployState ENUM ('SUCCEEDED', 'FAILED_INTERNAL_STATE', 'CANCELING', 'WAITING', 'OVERDUE', 'FAILED', 'CANCELED') NOT NULL
  ADD COLUMN `deployJson` JSON DEFAULT NULL;
