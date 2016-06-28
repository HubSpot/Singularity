import { PropTypes } from 'react';

const RequestPropTypes = {};

RequestPropTypes.Request = PropTypes.shape({
  id: PropTypes.string.isRequired,
  requestType: PropTypes.string.isRequired,
  owners: PropTypes.arrayOf(PropTypes.string),
  schedule: PropTypes.string,
  quartzSchedule: PropTypes.string,
  scheduleType: PropTypes.string,
  instances: PropTypes.number
});

RequestPropTypes.RequestDeployState = PropTypes.shape({
  activeDeploy: PropTypes.shape({
    deployId: PropTypes.string.isRequired,
    timestamp: PropTypes.number.isRequired,
    user: PropTypes.string,
    message: PropTypes.string
  }),
  pendingDeploy: PropTypes.shape({
    deployId: PropTypes.string.isRequired,
    timestamp: PropTypes.number.isRequired,
    user: PropTypes.string,
    message: PropTypes.string
  })
});

RequestPropTypes.RequestParent = PropTypes.shape({
  request: RequestPropTypes.Request.isRequired,
  state: PropTypes.string.isRequired,
  requestDeployState: RequestPropTypes.RequestDeployState,
});

export default RequestPropTypes;
