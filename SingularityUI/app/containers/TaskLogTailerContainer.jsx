import React from 'react';

import { connect } from 'react-redux';

import { setTailerGroups } from '../actions/tailer';
import { FetchActiveTasksForRequest } from '../actions/api/history';
import Utils from '../utils';

import LogTailerContainer from './LogTailerContainer';
import LessTailerContainer from './LessTailerContainer';

class TaskLogTailerContainer extends React.Component {
  componentWillMount() {
    this.props.setTailerGroups([[{
      taskId: this.props.params.taskId,
      path: this.props.params.splat,
      offset: this.props.location.query.offset || -1
    }]]);

    this.props.fetchActiveTasksForRequest(Utils.getRequestIdFromTaskId(this.props.params.taskId));
  }

  render() {
    if (this.props.route.path.includes('less')) {
      Utils.setPreferredTailer('less');
    } else {
      Utils.setPreferredTailer('tail');
    }

    if (Utils.isLessEnabled() && this.props.route.path.includes('less')) {
      return (<LessTailerContainer />);
    }

    return (<LogTailerContainer />);
  }
};

export default connect(null, {
  setTailerGroups,
  fetchActiveTasksForRequest: FetchActiveTasksForRequest.trigger
})(TaskLogTailerContainer);
