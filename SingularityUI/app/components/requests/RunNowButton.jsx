import React, { Component, PropTypes } from 'react';

import RunNowModal from '../common/RunNowModal';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import TaskLauncher from '../common/TaskLauncher';

export default class UnpauseButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    runAction: PropTypes.func.isRequired
  };

  handleRunNow(requestId, data) {
    this.props.runAction(requestId, data).then((response) => {
      if (_.contains([RunNowModal.AFTER_TRIGGER.SANDBOX.value, RunNowModal.AFTER_TRIGGER.TAIL.value], data.afterTrigger)) {
        this.refs.taskLauncher.startPolling(response.data.request.id, response.data.pendingRequest.runId, data.afterTrigger === RunNowModal.AFTER_TRIGGER.TAIL.value && data.fileToTail);
      }
    });
  }

  render() {
    return (
      <span>
        <a onClick={() => this.refs.runModal.show(this.props.requestId)}><Glyphicon iconClass="flash" /></a>
        <RunNowModal
          ref="runModal"
          onRunNow={(...args) => this.handleRunNow(...args)}
        />
        <TaskLauncher
          ref="taskLauncher"
          fetchTaskRun={(...args) => this.props.fetchRunAction(...args)}
          fetchTaskRunHistory={(...args) => this.props.fetchRunHistoryAction(...args)}
          fetchTaskFiles={(...args) => this.props.fetchTaskFilesAction(...args)}
        />
      </span>
    );
  }
}
