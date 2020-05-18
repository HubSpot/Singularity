package com.hubspot.singularity.auth.authenticator;

import com.ning.http.client.Response;
import java.io.IOException;

public abstract class WebhookResponseParser {

  abstract SingularityUserPermissionsResponse parse(Response response) throws IOException;
}
