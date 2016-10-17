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
