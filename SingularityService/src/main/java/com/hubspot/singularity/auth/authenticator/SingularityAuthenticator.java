package com.hubspot.singularity.auth.authenticator;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.hubspot.singularity.SingularityUser;

public interface SingularityAuthenticator extends Provider<Optional<SingularityUser>> {

}
