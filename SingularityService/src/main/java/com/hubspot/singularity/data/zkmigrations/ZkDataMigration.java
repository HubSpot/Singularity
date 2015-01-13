package com.hubspot.singularity.data.zkmigrations;

import java.util.Objects;

public abstract class ZkDataMigration implements Comparable<ZkDataMigration> {

  public abstract void applyMigration();

  private final int migrationNumber;

  public ZkDataMigration(int migrationNumber) {
    this.migrationNumber = migrationNumber;
  }

  @Override
  public String toString() {
    return String.format("%s - %s", migrationNumber, getClass().getSimpleName());
  }

  public int getMigrationNumber() {
    return migrationNumber;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(migrationNumber);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ZkDataMigration other = (ZkDataMigration) obj;
    if (migrationNumber != other.migrationNumber) {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(ZkDataMigration o) {
    return Integer.compare(migrationNumber, o.migrationNumber);
  }

}
