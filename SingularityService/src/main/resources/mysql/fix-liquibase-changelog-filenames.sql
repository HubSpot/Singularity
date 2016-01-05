--liquibase formatted sql

--changeset tpetr:1 dbms:mysql
UPDATE DATABASECHANGELOG SET FILENAME = 'mysql/singularity.sql' WHERE FILENAME != 'mysql/fix-liquibase-changelog-filenames.sql';
