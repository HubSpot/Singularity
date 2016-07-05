import React, { PropTypes } from 'react';
import { Button } from 'react-bootstrap';

import JSONButton from '../common/JSONButton';

import RemoveButton from '../requests/RemoveButton';

import Utils from '../../utils';

const RequestActionButtons = ({requestParent}) => {
  const {request, state} = requestParent;

  let maybeNewDeployButton;
  if (!config.hideNewDeployButton) {
    maybeNewDeployButton = (
      <Button href={`${config.appRoot}/request/${request.id}/deploy`} bsStyle="success">
        Deploy
      </Button>
    );
  }

  let maybeRunNowButton;
  if (Utils.request.canBeRunNow(requestParent)) {
    maybeRunNowButton = (
      <Button bsStyle="primary">
        Run now
      </Button>
    );
  }

  let maybeExitCooldownButton;
  if (state === 'SYSTEM_COOLDOWN') {
    maybeExitCooldownButton = (
      <Button bsStyle="primary">
        Exit Cooldown
      </Button>
    );
  }

  let maybeScaleButton;
  if (Utils.request.canBeScaled(requestParent)) {
    maybeScaleButton = (
      <Button bsStyle="primary" disabled={Utils.request.scaleDisabled(requestParent)}>
        Scale
      </Button>
    );
  }

  let togglePauseButton;
  if (state === 'PAUSED') {
    if (Utils.request.pauseDisabled(requestParent)) {
      // make sure the action removes the expiring pause
    }
    togglePauseButton = (
      <Button bsStyle="primary">
        Unpause
      </Button>
    );
  } else {
    togglePauseButton = (
      <Button bsStyle="primary" disabled={Utils.request.pauseDisabled(requestParent)}>
        Pause
      </Button>
    );
  }

  let maybeBounceButton;
  if (Utils.request.canBeBounced(requestParent)) {
    maybeBounceButton = (
      <Button bsStyle="primary" disabled={Utils.request.bounceDisabled(requestParent)}>
        Bounce
      </Button>
    );
  }

  let maybeEditButton;
  if (!config.hideNewRequestButton) {
    maybeEditButton = (
      <Button bsStyle="primary" href={`${config.appRoot}/requests/edit/${request.id}`}>
        Edit
      </Button>
    );
  }

  let maybeToggleHealthchecksButton;
  if (Utils.request.canDisableHealthchecks(requestParent)) {
    if (request.skipHealthchecks) {
      maybeToggleHealthchecksButton = (
        <Button bsStyle="warning">
          Enable Healthchecks
        </Button>
      );
    } else {
      maybeToggleHealthchecksButton = (
        <Button bsStyle="warning">
          Disable Healthchecks
        </Button>
      );
    }
  }

  let removeButton;
  removeButton = (
    <RemoveButton className="btn btn-danger" requestId={request.id}>
      Remove
    </RemoveButton>
  );

  return (
    <div>
      <JSONButton linkClassName="btn btn-default" object={requestParent}>JSON</JSONButton>
      {maybeNewDeployButton}
      {maybeRunNowButton}
      {maybeExitCooldownButton}
      {maybeScaleButton}
      {togglePauseButton}
      {maybeBounceButton}
      {maybeEditButton}
      {maybeToggleHealthchecksButton}
      {removeButton}
    </div>
  );
};

RequestActionButtons.propTypes = {
  requestParent: PropTypes.object.isRequired
};

export default RequestActionButtons;
