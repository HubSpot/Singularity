import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { FetchRequest } from '../actions/api/requests';
import { FetchActiveTasksForRequest } from '../actions/api/history';
import { FetchTaskCleanups } from '../actions/api/tasks';

import RequestHeader from './RequestHeader';
import TaskStateBreakdown from './TaskStateBreakdown';

class RequestDetailPage extends Component {
  render() {

    return (
      <div>
        <RequestHeader requestId={requestId} />
        <TaskStateBreakdown requestId={requestId} />
      </div>
    );
  }
}

RequestDetailPage.propTypes = {
  requestId: PropTypes.string.isRequired
};

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchRequest: FetchRequest.trigger(ownProps.requestId),
  fetchActiveTasksForRequest: FetchActiveTasksForRequest.trigger(ownProps.requestId),
  fetchTaskCleanups: FetchTaskCleanups.trigger()
});

export default connect(
  null,
  mapDispatchToProps
)(RequestDetailPage);
