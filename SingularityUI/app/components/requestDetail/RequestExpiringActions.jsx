import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Button } from 'react-bootstrap';

import Utils from '../../utils';

import {
  ScaleRequest,
  PersistRequestScale,
  PersistSkipRequestHealthchecks,
  CancelRequestBounce,
  PersistRequestPause
} from '../../actions/api/requests';

import UnpauseButton from '../common/modalButtons/UnpauseButton';
import EnableHealthchecksButton from '../common/modalButtons/EnableHealthchecksButton';
import DisableHealthchecksButton from '../common/modalButtons/DisableHealthchecksButton';

import ExpiringActionNotice from './ExpiringActionNotice';

const RequestExpiringActions = ({
  requestId,
  requestParent,
  scale,
  persistScale,
  cancelBounce,
  persistPause,
  persistSkipHealthchecks
}) => {
  let maybeScaleExpiration;
  if (requestParent.expiringScale) {
    const {
      expiringScale: {
        startMillis,
        user,
        revertToInstances,
        expiringAPIRequestObject: {
          durationMillis,
          message
        }
      },
      request: {
        instances
      }
    } = requestParent;
    const endMillis = startMillis + durationMillis;
    if (endMillis > new Date().getTime()) {
      maybeScaleExpiration = (
        <ExpiringActionNotice
          action={`Scale (to ${instances} instances)`}
          user={user ? user.split('@')[0] : ''}
          endMillis={endMillis}
          canRevert={true}
          persistText="Make Permanent"
          persistAction={persistScale}
          revertText={`Revert to ${revertToInstances} ${revertToInstances === 1 ? 'instance' : 'instances'}`}
          revertAction={() => scale(revertToInstances).then(persistScale())}
          message={message}
        />
      );
    }
  }

  let maybeBounceExpiration;
  if (requestParent.expiringBounce) {
    const {
      expiringBounce: {
        startMillis,
        user,
        expiringAPIRequestObject: {
          durationMillis,
          message
        }
      }
    } = requestParent;

    // TODO: waiting on #127 to prove default automatically
    const endMillis = startMillis + (durationMillis || (config.defaultBounceExpirationMinutes * 60 * 1000));
    if (endMillis > new Date().getTime()) {
      maybeBounceExpiration = (
        <ExpiringActionNotice
          action="Bounce"
          user={user ? user.split('@')[0] : ''}
          endMillis={endMillis}
          canRevert={false}
          persistText="Cancel bounce"
          persistAction={cancelBounce}
          message={message}
        />
      );
    }
  }

  let maybePauseExpiration;
  if (requestParent.expiringPause) {
    const {
      expiringPause: {
        startMillis,
        user,
        expiringAPIRequestObject: {
          durationMillis,
          message
        }
      }
    } = requestParent;
    const endMillis = startMillis + durationMillis;
    if (endMillis > new Date().getTime()) {
      maybePauseExpiration = (
        <ExpiringActionNotice
          action="Pause"
          user={user ? user.split('@')[0] : ''}
          endMillis={endMillis}
          canRevert={true}
          persistText="Make Permanent"
          persistAction={persistPause}
          revertButton={
            <UnpauseButton requestId={requestId}>
              <Button bsStyle="primary" bsSize="xsmall">Unpause</Button>
            </UnpauseButton>
          }
          message={message}
        />
      );
    }
  }

  let maybeSkipHealthchecksExpiration;
  if (requestParent.expiringSkipHealthchecks) {
    const {
      expiringSkipHealthchecks: {
        startMillis,
        user,
        expiringAPIRequestObject: {
          skipHealthchecks,
          durationMillis,
          message
        }
      }
    } = requestParent;
    const endMillis = startMillis + durationMillis;
    if (endMillis > new Date().getTime()) {
      let revertButton;
      if (skipHealthchecks) {
        revertButton = (
          <EnableHealthchecksButton requestId={requestId}>
            <Button bsStyle="primary" bsSize="xsmall">Enable Healthchecks</Button>
          </EnableHealthchecksButton>
        );
      } else {
        revertButton = (
          <DisableHealthchecksButton requestId={requestId}>
            <Button bsStyle="primary" bsSize="xsmall">Disable Healthchecks</Button>
          </DisableHealthchecksButton>
        );
      }

      maybeSkipHealthchecksExpiration = (
        <ExpiringActionNotice
          action={skipHealthchecks ? 'Disable Healthchecks' : 'Enable Healthchecks'}
          user={user ? user.split('@')[0] : ''}
          endMillis={endMillis}
          canRevert={true}
          persistText="Make Permanent"
          persistAction={persistSkipHealthchecks}
          revertButton={revertButton}
          message={message}
        />
      );
    }
  }

  return (
    <div>
      {maybeScaleExpiration}
      {maybeBounceExpiration}
      {maybePauseExpiration}
      {maybeSkipHealthchecksExpiration}
    </div>
  );
};

RequestExpiringActions.propTypes = {
  requestId: PropTypes.string.isRequired,
  requestParent: PropTypes.object.isRequired,
  scale: PropTypes.func.isRequired,
  persistScale: PropTypes.func.isRequired,
  cancelBounce: PropTypes.func.isRequired,
  persistPause: PropTypes.func.isRequired,
  persistSkipHealthchecks: PropTypes.func.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  requestParent: Utils.maybe(state.api.request, [ownProps.requestId, 'data'])
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  scale: (instances) => dispatch(ScaleRequest.trigger(ownProps.requestId, {instances})),
  persistScale: () => dispatch(PersistRequestScale.trigger(ownProps.requestId)),
  cancelBounce: () => dispatch(CancelRequestBounce.trigger(ownProps.requestId)),
  persistPause: () => dispatch(PersistRequestPause.trigger(ownProps.requestId)),
  persistSkipHealthchecks: () => dispatch(PersistSkipRequestHealthchecks.trigger(ownProps.requestId)),
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestExpiringActions);
