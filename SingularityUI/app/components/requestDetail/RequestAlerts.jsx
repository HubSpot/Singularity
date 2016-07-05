import React, { PropTypes } from 'react';
import { Well, Alert } from 'react-bootstrap';

const RequestAlerts = ({requestParent, bounces}) => {
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
  requestParent: PropTypes.object.isRequired,
  bounces: PropTypes.arrayOf(PropTypes.object).isRequired
};

export default RequestAlerts;
