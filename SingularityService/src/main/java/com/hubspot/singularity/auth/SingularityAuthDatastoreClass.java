package com.hubspot.singularity.auth;

import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.auth.datastore.SingularityDisabledAuthDatastore;
import com.hubspot.singularity.auth.datastore.SingularityDummyDatastore;
import com.hubspot.singularity.auth.datastore.SingularityLDAPDatastore;

public enum SingularityAuthDatastoreClass {
  LDAP(SingularityLDAPDatastore.class),
  DISABLED(SingularityDisabledAuthDatastore.class),
  DUMMY(SingularityDummyDatastore.class);

  private final Class<? extends SingularityAuthDatastore> authDatastoreClass;

  SingularityAuthDatastoreClass(Class<? extends SingularityAuthDatastore> authDatastoreClass) {
    this.authDatastoreClass = authDatastoreClass;
  }

  public Class<? extends SingularityAuthDatastore> getAuthDatastoreClass() {
    return authDatastoreClass;
  }
}
