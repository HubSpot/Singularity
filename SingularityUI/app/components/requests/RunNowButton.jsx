import React, { Component, PropTypes } from 'react';
import { withRouter } from 'react-router';
import { connect } from 'react-redux';
import { FetchTaskHistory } from '../../actions/api/history';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import RunNowModal from './RunNowModal';
import { getClickComponent } from '../common/modal/ModalWrapper';
import Utils from '../../utils';

const runNowTooltip = (
  <ToolTip id="run-now">
    Run Now
  </ToolTip>
);

class RunNowButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    fetchTaskHistory: PropTypes.func.isRequired,
    children: PropTypes.node,
    router: PropTypes.object,
    taskId: PropTypes.string,
    task: PropTypes.object
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-run-now-overlay" overlay={runNowTooltip}>
        <a title="Run Now">
          <Glyphicon glyph="flash" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        <span>{getClickComponent(this, this.props.taskId && (() => this.props.fetchTaskHistory(this.props.taskId)))}</span>
        <RunNowModal ref="modal" requestId={this.props.requestId} task={this.props.task} router={this.props.router} />
      </span>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  task: ownProps.taskId && Utils.maybe(state.api.task[ownProps.taskId], ['data', 'task'])
});

const mapDispatchToProps = (dispatch) => ({
  fetchTaskHistory: (taskId) => dispatch(FetchTaskHistory.trigger(taskId))
});

export default connect(mapStateToProps, mapDispatchToProps)(withRouter(RunNowButton));
