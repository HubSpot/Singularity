import React from 'react';

import { connect } from 'react-redux';

import { setTailerGroups } from '../actions/tailer';

import LogTailerContainer from './LogTailerContainer';

import Utils from '../utils';

class TaskLogTailerContainer extends React.Component {
  componentWillMount() {
    // TODO: populate task dropdown
    this.props.setTailerGroups([[{
      taskId: this.props.params.taskId,
      path: this.props.params.splat,
      offset: this.props.location.query.offset || -1
    }]]);
  }

  render() {
    return (<LogTailerContainer />);
  }
};

export default connect(null, {
  setTailerGroups
})(TaskLogTailerContainer);
