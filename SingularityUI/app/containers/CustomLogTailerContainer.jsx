import React from 'react';

import { connect } from 'react-redux';

import { setTailerGroups } from '../actions/tailer';

import LogTailerContainer from './LogTailerContainer';
import Utils from '../utils';

class CustomLogTailerContainer extends React.Component {
  componentWillMount() {
    const taskIds = Utils.maybe(this.props.location.query, ['taskIds'], '').split(',');
    const unifiedView = this.props.location.query.unified === 'true';

    // TODO: error if no task ids
    const tg = taskIds.map((taskId) => ({taskId, path: this.props.params.splat, offset: -1}));

    if (unifiedView) {
      this.props.setTailerGroups([tg]);
    } else {
      this.props.setTailerGroups(tg.map((item) => [item]));
    }
  }

  render() {
    return (<LogTailerContainer />);
  }
};

export default connect(null, {
  setTailerGroups,
})(CustomLogTailerContainer);
