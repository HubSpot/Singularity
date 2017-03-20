import React from 'react';
import { Link } from 'react-router';
import Utils from '../../utils';

const NewTaskGroupHeader = ({showCloseAndExpandButtons, showRequestId, taskId, onClose, onExpand, onJumpToTop, onJumpToBottom}) => {
  const closeComponent = (<a className="action-link" onClick={onClose} title="Close Task">
    <span className="glyphicon glyphicon-remove" />
  </a>);

  const expandComponent = (<a className="action-link" onClick={onExpand} title="Show only this Task">
    <span className="glyphicon glyphicon-resize-full" />
  </a>);

  const { requestId, instanceNo } = Utils.getTaskDataFromTaskId(taskId);

  const taskInfoTitle = showRequestId
    ? `${requestId} ${instanceNo}`
    : `Instance ${instanceNo}`;

  return (
    <header>
      <div className="individual-header">
        { showCloseAndExpandButtons && closeComponent }
        { showCloseAndExpandButtons && expandComponent }
        <span>
          <div className="width-constrained">
          <Link to={`/task/${taskId}`} title={taskId}>{ taskInfoTitle }</Link>
          </div>
        </span>
        { /* this.renderTaskLegend() */ }
        <span className="right-buttons">
          <a className="action-link" onClick={onJumpToBottom} title="Scroll to bottom">
            <span className="glyphicon glyphicon-chevron-down" />
          </a>
          <a className="action-link" onClick={onJumpToTop} title="Scroll to top">
            <span className="glyphicon glyphicon-chevron-up" />
          </a>
        </span>
      </div>
    </header>);
}

NewTaskGroupHeader.propTypes = {
  onClose: React.PropTypes.func.isRequired,
  onExpand: React.PropTypes.func.isRequired,
  onJumpToTop: React.PropTypes.func.isRequired,
  onJumpToBottom: React.PropTypes.func.isRequired,

  showCloseAndExpandButtons: React.PropTypes.bool.isRequired,
  taskId: React.PropTypes.string.isRequired,
};

export default NewTaskGroupHeader;
