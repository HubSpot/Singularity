import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Well, Alert } from 'react-bootstrap';

import Utils from '../../utils';

import { getBouncesForRequest } from '../../selectors/tasks';

const RequestAlerts = ({requestParent, bounces, activeTasksForRequest}) => {
  let maybeBouncing;
  if (bounces.length > 0) {
    maybeBouncing = (
      <Alert bsStyle="warning">
        <b>Request is bouncing:</b> {runningInstanceCount} of {requestParent.request.instances} replacement tasks are currently running.
      </Alert>
    );
  }


  let runningInstanceCount = 0;
  return (
    <div>
      {maybeBouncing}
    </div>
  );
};

RequestAlerts.propTypes = {
  requestId: PropTypes.string.isRequired,
  requestParent: PropTypes.object.isRequired,
  bounces: PropTypes.arrayOf(PropTypes.object).isRequired,
  activeTasksForRequest: PropTypes.arrayOf(PropTypes.object).isRequired
};

const mapStateToProps = (state, ownProps) => {
  return {
    requestParent: Utils.maybe(state.api.request, [ownProps.requestId, 'data']),
    bounces: getBouncesForRequest(ownProps.requestId)(state),
    activeTasksForRequest: Utils.maybe(state.api, ['activeTasksForRequest', ownProps.requestId, 'data'])
  };
};


export default connect(
  mapStateToProps
)(RequestAlerts);
