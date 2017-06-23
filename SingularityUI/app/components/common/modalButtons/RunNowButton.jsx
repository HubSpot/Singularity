import React, { Component, PropTypes } from 'react';
import { withRouter } from 'react-router';
import { connect } from 'react-redux';
import { FetchTaskHistory, FetchTaskHistoryForRequest, FetchRequestArgHistory } from '../../../actions/api/history';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import RunNowModal from './RunNowModal';
import { getClickComponent } from '../modal/ModalWrapper';
import Utils from '../../../utils';

const runNowTooltip = (
  <ToolTip id="run-now">
    Run Now
  </ToolTip>
);

class RunNowButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    fetchTaskHistory: PropTypes.func.isRequired,
    fetchLastRunTask: PropTypes.func.isRequired,
    fetchRequestArgHistory: PropTypes.func.isRequired,
    taskHistory: PropTypes.arrayOf(PropTypes.shape({
      taskId: PropTypes.shape({
        id: PropTypes.string.isRequired
      }).isRequired
    })),
    requestArgHistory: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)).isRequired,
    children: PropTypes.node,
    router: PropTypes.object,
    taskId: PropTypes.string,
    task: PropTypes.object,
    then: PropTypes.func
  };

  constructor(props) {
    super(props);
    _.bindAll(this, 'doBeforeOpeningModal');
  }

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-run-now-overlay" overlay={runNowTooltip}>
        <a title="Run Now">
          <Glyphicon glyph="flash" />
        </a>
      </OverlayTrigger>
    )
  };

  doBeforeOpeningModal() {
    if (this.props.taskId) {
      return this.props.fetchRequestArgHistory(this.props.requestId).then(this.props.fetchTaskHistory(this.props.taskId));
    }
    if (!_.isEmpty(this.props.taskHistory)) {
      return this.props.fetchRequestArgHistory(this.props.requestId).then(this.props.fetchTaskHistory(this.props.taskHistory[0].taskId.id));
    }
    return this.props.fetchLastRunTask(this.props.requestId).then((response) => {
      const taskId = Utils.maybe(response, ['data', '0', 'taskId', 'id']);
      if (taskId) {
        return this.props.fetchRequestArgHistory(this.props.requestId).then(this.props.fetchTaskHistory(taskId));
      }
      return Promise.resolve();
    });
  }

  render() {
    return (
      <span>
        <span>{getClickComponent(this, this.doBeforeOpeningModal)}</span>
        <RunNowModal
          ref="modal"
          requestId={this.props.requestId}
          task={this.props.task}
          rerun={!!this.props.taskId}
          router={this.props.router}
          then={this.props.then}
          requestArgHistory={this.props.requestArgHistory}
        />
      </span>
    );
  }
}

const mapStateToProps = (state, ownProps) => {
  const taskHistory = Utils.maybe(state.api.taskHistoryForRequest, [ownProps.requestId, 'data'], []);
  const taskId = ownProps.taskId || taskHistory[0] && taskHistory[0].taskId.id;
  return {
    taskHistory,
    task: Utils.maybe(state.api.task[taskId], ['data', 'task']),
    requestArgHistory: Utils.maybe(state.api.requestArgHistory, [ownProps.requestId, 'data'], [])
  };
};

const mapDispatchToProps = (dispatch) => ({
  fetchLastRunTask: (requestId) => dispatch(FetchTaskHistoryForRequest.trigger(requestId, 1, 1, [404])),
  fetchTaskHistory: (taskId) => dispatch(FetchTaskHistory.trigger(taskId), false, [404]),
  fetchRequestArgHistory: (requestId) => dispatch(FetchRequestArgHistory.trigger(requestId))
});

export default connect(mapStateToProps, mapDispatchToProps)(withRouter(RunNowButton));
