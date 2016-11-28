import React from 'react';
import TaskStatusIndicator from './TaskStatusIndicator';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import Utils from '../../utils';
import { Link } from 'react-router';

import { connect } from 'react-redux';

import { jumpToTop, jumpToBottom } from '../../actions/tailer';

class NewTaskGroupHeader extends React.Component {
  renderClose() {
    if (this.props.taskGroupsCount > 1) {
      return React.createElement('a', { 'className': 'action-link', ['onClick']: () => this.props.removeTaskGroup(this.props.taskGroupId), 'title': 'Close Task' }, <span className="glyphicon glyphicon-remove" />);
    }
  }

  renderExpand() {
    if (this.props.taskGroupsCount > 1) {
      return React.createElement('a', { 'className': 'action-link', ['onClick']: () => this.props.expandTaskGroup(this.props.taskGroupId), 'title': 'Show only this Task' }, <span className="glyphicon glyphicon-resize-full" />);
    }
  }

  render() {
    return (<div className="individual-header">
      { this.renderClose() }
      { this.renderExpand() }
      <span>
        <div className="width-constrained">
          <Link className="instance-link" to={`task/${ this.props.taskId }`}>{ this.props.taskId }</Link>
        </div>
      </span>
      { /* this.renderTaskLegend() */ }
      <span className="right-buttons">
        <a className="action-link" onClick={() => this.props.jumpToBottom(this.props.tailerId, this.props.taskId, this.props.path)} title="Scroll to bottom">
          <span className="glyphicon glyphicon-chevron-down" />
        </a>
        <a className="action-link" onClick={() => this.props.jumpToTop(this.props.tailerId, this.props.taskId, this.props.path)} title="Scroll to top">
          <span className="glyphicon glyphicon-chevron-up" />
        </a>
      </span>
    </div>);
  }
}

NewTaskGroupHeader.propTypes = {
  tailerId: React.PropTypes.string.isRequired,
  taskId: React.PropTypes.string.isRequired,
  path: React.PropTypes.string.isRequired,
};

export default connect(null, {
  jumpToTop,
  jumpToBottom
})(NewTaskGroupHeader);
