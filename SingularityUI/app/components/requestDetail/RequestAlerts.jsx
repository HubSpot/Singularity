import React, { PropTypes } from 'react';
import { Well, Alert } from 'react-bootstrap';

const RequestAlerts = ({requestParent}) => {
  let runningInstanceCount = 0;
  return (
    <div>
      <Alert bsStyle="warning">
        <b>Request is bouncing:</b> {runningInstanceCount} of {requestParent.request.instances} replacement tasks are currently running.
      </Alert>
    </div>
  );
};

RequestAlerts.propTypes = {
  requestParent: PropTypes.object.isRequired,
  bounces: PropTypes.arrayOf(PropTypes.object).isRequired
};

export default RequestAlerts;
