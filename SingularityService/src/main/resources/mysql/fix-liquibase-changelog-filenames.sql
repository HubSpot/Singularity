--liquibase formatted sql

--changeset tpetr:0 dbms:mysql
UPDATE DATABASECHANGELOG SET FILENAME = 'mysql/singularity.sql' WHERE ID < 9;
