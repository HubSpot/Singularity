--liquibase formatted sql

--changeset tpetr:1 dbms:mysql
CREATE TABLE `taskHistory` (
  `taskId` varchar(200) NOT NULL DEFAULT '',
  `requestId` varchar(100) NOT NULL,
  `status` varchar(50) NOT NULL,
  `pendingType` varchar(50) NOT NULL,
  `createdAt` timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  `lastTaskStatus` varchar(100) DEFAULT NULL,
  `updatedAt` timestamp NULL DEFAULT NULL,
  `directory` varchar(500) DEFAULT NULL,
  `task` blob NOT NULL,
  PRIMARY KEY (`taskId`),
  KEY `requestId` (`requestId`,`createdAt`),
  KEY `requestId_2` (`requestId`,`updatedAt`),
  KEY `requestId_3` (`requestId`,`lastTaskStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `requestHistory` (
  `requestId` varchar(100) NOT NULL,
  `createdAt` timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  `requestState` varchar(25) NOT NULL,
  `user` varchar(100) DEFAULT NULL,
  `request` blob NOT NULL,
  PRIMARY KEY (`requestId`,`createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `taskUpdates` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `taskId` varchar(200) DEFAULT NULL,
  `status` varchar(100) NOT NULL,
  `message` varchar(200) DEFAULT NULL,
  `createdAt` timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  PRIMARY KEY (`id`),
  KEY `taskId` (`taskId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--changeset tpetr:2 dbms:mysql
CREATE TABLE `deployHistory` (
  `requestId` varchar(100) NOT NULL,
  `deployId` varchar(100) NOT NULL,
  `createdAt` timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  `user` varchar(100) DEFAULT NULL,
  `deployStateAt` timestamp NOT NULL DEFAULT '1971-01-01 00:00:01',
  `deployState` varchar(25) NOT NULL,
  `bytes` blob NOT NULL,
  PRIMARY KEY (`requestId`,`deployId`),
  KEY `requestId` (`requestId`,`createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--changeset tpetr:3 dbms:mysql
ALTER TABLE `taskHistory`
  DROP COLUMN `status`,
  DROP COLUMN `pendingType`,
  DROP COLUMN `createdAt`,
  DROP COLUMN `directory`,
  MODIFY `lastTaskStatus` VARCHAR(25) DEFAULT NULL,
  MODIFY `updatedAt` TIMESTAMP NOT NULL DEFAULT '1971-01-01 00:00:01',
  CHANGE `task` `bytes` BLOB NOT NULL,
  DROP KEY `requestId`,
  DROP KEY `requestId_3`;

DROP TABLE `taskUpdates`;

--changeset tpetr:4 dbms:mysql
ALTER TABLE `deployHistory` ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8;

ALTER TABLE `requestHistory` ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8;

ALTER TABLE `taskHistory` ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8;

--changeset ssalinas:5 dbms:mysql
ALTER TABLE `taskHistory` MODIFY `bytes` MEDIUMBLOB NOT NULL;

--changeset wsorenson:6 dbms:mysql
ALTER TABLE `taskHistory` ADD COLUMN runId VARCHAR(100) NULL;

--changeset ssalinas:7 dbms:mysql
ALTER TABLE `taskHistory`
  ADD COLUMN deployId VARCHAR(100) NULL,
  ADD KEY `deployId` (`deployId`, `requestId`, `updatedAt`);
UPDATE `taskHistory` SET `deployId` = SUBSTRING_INDEX(SUBSTRING_INDEX(`taskId`, '-', -5), '-', 1) WHERE `deployId` IS NULL;

--changeset ssalinas:8 dbms:mysql
ALTER TABLE `taskHistory` ADD KEY `runId` (`runId`, `requestId`);

--changeset wsorenson:9 dbms:mysql
ALTER TABLE `requestHistory` ADD COLUMN message VARCHAR(280) NULL;

ALTER TABLE `deployHistory` ADD COLUMN message VARCHAR(280) NULL;

--changeset wsorenson:10 dbms:mysql
ALTER TABLE `taskHistory`
  ADD COLUMN `host` VARCHAR(100) CHARACTER SET ASCII NULL,
  ADD COLUMN `startedAt` TIMESTAMP NULL,
  DROP KEY `deployId`,
  ADD KEY `startedAt` (`requestId`, `startedAt`),
  ADD KEY `lastTaskStatus` (`requestId`, `lastTaskStatus`, `startedAt`),
  ADD KEY `deployId` (`requestId`, `deployId`, `startedAt`),
  ADD KEY `host` (`requestId`, `host`, `startedAt`),
  ADD KEY `startedAt2` (`startedAt`, `requestId`);

--changeset wsorenson:11 dbms:mysql
UPDATE `taskHistory` SET `host` = SUBSTRING_INDEX(SUBSTRING_INDEX(`taskId`, '-', -2), '-', 1) WHERE `host` IS NULL;
UPDATE `taskHistory` SET `startedAt` = FROM_UNIXTIME(SUBSTRING_INDEX(SUBSTRING_INDEX(`taskId`, '-', -4), '-', 1)/1000) WHERE `startedAt` IS NULL;

--changeset ssalinas:12 dbms:mysql
ALTER TABLE `taskHistory`
  ADD KEY `updatedAt` (`updatedAt`, `requestId`)

--changeset ssalinas:13 dbms:mysql
ALTER TABLE `deployHistory` MODIFY `bytes` MEDIUMBLOB NOT NULL;

--changeset tpetr:14 dbms:mysql
ALTER TABLE `requestHistory` MODIFY `createdAt` TIMESTAMP(3) NOT NULL DEFAULT '1971-01-01 00:00:01'

--changeset ssalinas:15 dbms:mysql
ALTER TABLE `taskHistory`
  ADD COLUMN `purged` BOOLEAN NOT NULL DEFAULT false,
  ADD KEY `purged` (`requestId`, `purged`, `updatedAt`);

--changeset ssalinas:16 dbms:mysql
ALTER TABLE `taskHistory`
  DROP KEY `startedAt2`,
  ADD KEY `startedAt3` (`startedAt`)

--changeset ssalinas:17 dbms:mysql
CREATE TABLE `taskUsage` (
  `requestId` varchar(100) NOT NULL,
  `taskId` varchar(200) NOT NULL DEFAULT '',
  `memoryTotalBytes` BIGINT UNSIGNED NOT NULL,
  `cpuSeconds` DOUBLE UNSIGNED NOT NULL,
  `cpusThrottledTimeSecs` DOUBLE UNSIGNED NOT NULL,
  `diskTotalBytes` BIGINT UNSIGNED NOT NULL,
  `timestamp` BIGINT UNSIGNED NOT NULL,
  `cpusNrPeriods` BIGINT UNSIGNED NOT NULL,
  `cpusNrThrottled` BIGINT UNSIGNED NOT NULL,
   PRIMARY KEY (`taskId`, `timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--changeset ssalinas:18 dbms:mysql
ALTER TABLE `taskUsage` CHARACTER SET ascii COLLATE ascii_bin ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8;
ALTER TABLE `taskUsage`
  MODIFY COLUMN `requestId` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '',
  MODIFY COLUMN `taskId` varchar(200) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '';

--changeset ssalinas:19 dbms:mysql
ALTER TABLE `taskHistory` CHARACTER SET ascii COLLATE ascii_bin;
ALTER TABLE `taskHistory`
  MODIFY COLUMN `bytes` MEDIUMBLOB NOT NULL DEFAULT '',
  MODIFY COLUMN `taskId` varchar(200) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN `requestId` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN `lastTaskStatus` ENUM ('TASK_LAUNCHED', 'TASK_STAGING', 'TASK_STARTING', 'TASK_RUNNING', 'TASK_CLEANING', 'TASK_KILLING', 'TASK_FINISHED', 'TASK_FAILED', 'TASK_KILLED', 'TASK_LOST', 'TASK_LOST_WHILE_DOWN', 'TASK_ERROR', 'TASK_DROPPED', 'TASK_GONE', 'TASK_UNREACHABLE', 'TASK_GONE_BY_OPERATOR', 'TASK_UNKNOWN') NOT NULL,
  MODIFY COLUMN `runId` varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  MODIFY COLUMN `deployId` varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  ADD COLUMN `json` JSON DEFAULT NULL,
  ADD KEY `requestDeployUpdated` (`requestId`, `deployId`, `updatedAt`),
  ADD KEY `hostUpdated` (`host`, `updatedAt`);

--changeset ssalinas:20 dbms:mysql
ALTER TABLE `requestHistory` CHARACTER SET ascii COLLATE ascii_bin;
ALTER TABLE `requestHistory`
  MODIFY COLUMN `request` blob NOT NULL DEFAULT '',
  MODIFY COLUMN `requestId` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN `requestState` ENUM ('CREATED', 'UPDATED', 'DELETING', 'DELETED', 'PAUSED', 'UNPAUSED', 'ENTERED_COOLDOWN', 'EXITED_COOLDOWN', 'FINISHED', 'DEPLOYED_TO_UNPAUSE', 'BOUNCED', 'SCALED', 'SCALE_REVERTED') NOT NULL,
  MODIFY COLUMN `user` varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  MODIFY COLUMN `message` varchar(280) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `json` JSON DEFAULT NULL;

--changeset ssalinas:21 dbms:mysql
ALTER TABLE `deployHistory` CHARACTER SET ascii COLLATE ascii_bin;
ALTER TABLE `deployHistory`
  MODIFY COLUMN `bytes` MEDIUMBLOB NOT NULL DEFAULT '',
  MODIFY COLUMN `requestId` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN `deployId` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN `user` varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  MODIFY COLUMN `message` varchar(280) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  MODIFY COLUMN `deployState` ENUM ('SUCCEEDED', 'FAILED_INTERNAL_STATE', 'CANCELING', 'WAITING', 'OVERDUE', 'FAILED', 'CANCELED') NOT NULL,
  ADD COLUMN `json` JSON DEFAULT NULL;
