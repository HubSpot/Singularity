import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';

import { Button } from 'react-bootstrap';
import { Link } from 'react-router';

import JSONButton from '../../common/JSONButton';

import { FetchRequest } from '../../../actions/api/requests';

import RunNowButton from '../../requests/RunNowButton';
import RemoveButton from '../../requests/RemoveButton';
import PauseButton from '../../requests/PauseButton';
import UnpauseButton from '../../requests/UnpauseButton';
import BounceButton from '../../requests/BounceButton';
import ScaleButton from '../../requests/ScaleButton';
import ExitCooldownButton from '../../requests/ExitCooldownButton';
import EnableHealthchecksButton from '../../requests/EnableHealthchecksButton';
import DisableHealthchecksButton from '../../requests/DisableHealthchecksButton';

import Utils from '../../../utils';

const RequestActionButtons = ({requestParent, fetchRequest, router}) => {
  if (!requestParent || !requestParent.request) {
    return null;
  }
  const {request, state} = requestParent;

  let maybeNewDeployButton;
  if (!config.hideNewDeployButton) {
    maybeNewDeployButton = (
      <Link to={`request/${request.id}/deploy`}>
        <Button bsStyle="success">
          Deploy
        </Button>
      </Link>
    );
  }

  let maybeRunNowButton;
  if (Utils.request.canBeRunNow(requestParent)) {
    maybeRunNowButton = (
      <RunNowButton requestId={request.id} then={fetchRequest}>
        <Button bsStyle="primary">
          Run now
        </Button>
      </RunNowButton>
    );
  }

  let maybeExitCooldownButton;
  if (state === 'SYSTEM_COOLDOWN') {
    maybeExitCooldownButton = (
      <ExitCooldownButton requestId={request.id} then={fetchRequest}>
        <Button bsStyle="primary">
          Exit Cooldown
        </Button>
      </ExitCooldownButton>
    );
  }

  let maybeScaleButton;
  if (Utils.request.canBeScaled(requestParent)) {
    maybeScaleButton = (
      <ScaleButton requestId={request.id} currentInstances={request.instances} then={fetchRequest}>
        <Button bsStyle="primary" disabled={Utils.request.scaleDisabled(requestParent)}>
          Scale
        </Button>
      </ScaleButton>
    );
  }

  let togglePauseButton;
  if (state === 'PAUSED') {
    if (Utils.request.pauseDisabled(requestParent)) {
      // make sure the action removes the expiring pause
    }
    togglePauseButton = (
      <UnpauseButton requestId={request.id} then={fetchRequest}>
        <Button bsStyle="primary">
          Unpause
        </Button>
      </UnpauseButton>
    );
  } else {
    togglePauseButton = (
      <PauseButton requestId={request.id} isScheduled={request.requestType === 'SCHEDULED'} then={fetchRequest}>
        <Button bsStyle="primary" disabled={Utils.request.pauseDisabled(requestParent)}>
          Pause
        </Button>
      </PauseButton>
    );
  }

  let maybeBounceButton;
  if (Utils.request.canBeBounced(requestParent)) {
    maybeBounceButton = (
      <BounceButton requestId={request.id} then={fetchRequest}>
        <Button bsStyle="primary" disabled={Utils.request.bounceDisabled(requestParent)}>
          Bounce
        </Button>
      </BounceButton>
    );
  }

  let maybeEditButton;
  if (!config.hideNewRequestButton) {
    maybeEditButton = (
      <Link to={`requests/edit/${request.id}`}>
        <Button bsStyle="primary">
          Edit
        </Button>
      </Link>
    );
  }

  let maybeToggleHealthchecksButton;
  if (Utils.request.canDisableHealthchecks(requestParent)) {
    if (request.skipHealthchecks) {
      maybeToggleHealthchecksButton = (
        <EnableHealthchecksButton requestId={request.id} then={fetchRequest}>
          <Button bsStyle="warning">
            Enable Healthchecks
          </Button>
        </EnableHealthchecksButton>
      );
    } else {
      maybeToggleHealthchecksButton = (
        <DisableHealthchecksButton requestId={request.id} then={fetchRequest}>
          <Button bsStyle="primary">
            Disable Healthchecks
          </Button>
        </DisableHealthchecksButton>
      );
    }
  }

  const navigateAwayOnSuccess = (response) => {
    if (response.statusCode === 200) {
      router.push('/requests');
    }
  };

  const removeButton = (
    <RemoveButton requestId={request.id} then={navigateAwayOnSuccess}>
      <Button bsStyle="danger">
        Remove
      </Button>
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
  requestId: PropTypes.string.isRequired,
  requestParent: PropTypes.object,
  fetchRequest: PropTypes.func.isRequired,
  router: PropTypes.shape({push: PropTypes.func.isRequired}).isRequired
};

const mapStateToProps = (state, ownProps) => ({
  requestParent: Utils.maybe(state.api.request, [ownProps.requestId, 'data'])
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchRequest: () => dispatch(FetchRequest.trigger(ownProps.requestId))
});

export default withRouter(connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestActionButtons));
