import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { RunRequest } from '../../actions/api/requests';

import TaskLauncher from '../common/TaskLauncher';
import FormModal from '../common/FormModal';

import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

class RunNowModal extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    runNow: PropTypes.func.isRequired
  };

  static AFTER_TRIGGER = {
    STAY: {label: 'Stay on this page', value: 'STAY'},
    SANDBOX: {label: 'Wait for task to start, then browse its sandbox', value: 'SANDBOX'},
    TAIL: {label: 'Wait for task to start, then start tailing:', value: 'TAIL'}
  };

  show() {
    this.refs.runNowModal.show();
  }

  handleRunNow(data) {
    this.props.runNow(data).then((response) => {
      if (_.contains([RunNowModal.AFTER_TRIGGER.SANDBOX.value, RunNowModal.AFTER_TRIGGER.TAIL.value], data.afterTrigger)) {
        this.refs.taskLauncher.getWrappedInstance().startPolling(
          response.data.request.id,
          response.data.pendingRequest.runId,
          data.afterTrigger === RunNowModal.AFTER_TRIGGER.TAIL.value && data.fileToTail
        );
      }
    });
  }

  render() {
    return (
      <div>
        <TaskLauncher
          ref="taskLauncher"
        />
        <FormModal
          ref="runNowModal"
          action={<span><Glyphicon iconClass="flash" /> Run Task</span>}
          onConfirm={(data) => this.handleRunNow(data)}
          buttonStyle="primary"
          formElements={[
            {
              name: 'commandLineArgs',
              type: FormModal.INPUT_TYPES.TAGS,
              label: 'Additional command line input: (optional)'
            },
            {
              name: 'message',
              type: FormModal.INPUT_TYPES.STRING,
              label: 'Message: (optional)'
            },
            {
              name: 'afterTrigger',
              type: FormModal.INPUT_TYPES.RADIO,
              label: 'After triggering the run:',
              values: _.values(RunNowModal.AFTER_TRIGGER),
              defaultValue: RunNowModal.AFTER_TRIGGER.SANDBOX.value
            },
            {
              name: 'fileToTail',
              type: FormModal.INPUT_TYPES.STRING,
              defaultValue: _.rest(config.runningTaskLogPath.split('/'), '1').join('/')
            }
          ]}>
          <span>
            <p>Are you sure you want to immediately launch a task for this request?</p>
            <pre>{this.props.requestId}</pre>
          </span>
        </FormModal>
      </div>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  runNow: (data) => dispatch(RunRequest.trigger(ownProps.requestId, data)),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RunNowModal);
