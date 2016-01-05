--liquibase formatted sql

--changeset tpetr:10 dbms:mysql
UPDATE DATABASECHANGELOG SET FILENAME = 'mysql/singularity.sql' WHERE ID < 10;
