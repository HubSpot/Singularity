import React from 'react';
import { connect } from 'react-redux';

import _ from 'underscore';
import { FetchActiveTasksForRequest } from '../actions/api/history';
import { setTailerGroups } from '../actions/tailer';
import LessTailerContainer from './LessTailerContainer';
import LogTailerContainer from './LogTailerContainer';
import Utils from '../utils';

class RequestLogTailerContainer extends React.Component {
  componentWillMount() {
    const instances = this.props.location.query.instance
      ? this.props.location.query.instance.split(',').map(Number)
      : [1,2,3];

    const unifiedView = false; // this.props.location.query.unified === 'true';

    this.props.fetchActiveTasksForRequest(this.props.params.requestId).then((data) => {
      const tasksByInstanceNumber = _.groupBy(data.data, ({taskId}) => taskId.instanceNo);

      instances.sort();

      const tg = instances.filter((instanceNo) => tasksByInstanceNumber[instanceNo]).map((instanceNo) => ({
        taskId: tasksByInstanceNumber[instanceNo][0].taskId.id,
        path: this.props.params.splat,
        offset: -1
      }));

      if (unifiedView) {
        this.props.setTailerGroups([tg]);
      } else {
        this.props.setTailerGroups(tg.map((item) => [item]));
      }
    })
  }

  render() {
    if (this.props.route.path.includes('less')) {
      Utils.setPreferredTailer('less');
    } else if (this.props.route.path.includes('old-tail')) {
      Utils.setPreferredTailer('old-tail');
    } else {
      Utils.setPreferredTailer('tail');
    }
    
    if (Utils.isLessEnabled() && this.props.route && this.props.route.path.includes('less')) {
      return (<LessTailerContainer />);
    }

    return (<LogTailerContainer />);
  }
};

export default connect(null, {
  setTailerGroups,
  fetchActiveTasksForRequest: FetchActiveTasksForRequest.trigger
})(RequestLogTailerContainer);
