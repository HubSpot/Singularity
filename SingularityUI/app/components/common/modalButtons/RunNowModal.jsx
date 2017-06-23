import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { Glyphicon } from 'react-bootstrap';

import { RunRequest } from '../../../actions/api/requests';

import TaskLauncher from '../TaskLauncher';
import FormModal from '../modal/FormModal';

import Messenger from 'messenger';
import Utils from '../../../utils';
import uuid from 'uuid';

const LOCAL_STORAGE_AFTER_TRIGGER_VALUE = 'afterRunNowTrigger';
const LOCAL_STORAGE_TAIL_AFTER_TRIGGER_FILENAME = 'taskRunRedirectFilename';

class RunNowModal extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    runNow: PropTypes.func.isRequired,
    router: PropTypes.object.isRequired,
    rerun: PropTypes.bool,
    task: PropTypes.object,
    then: PropTypes.func,
    requestArgHistory: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)).isRequired
  };

  static AFTER_TRIGGER = {
    STAY: {label: 'Stay on this page', value: 'STAY'},
    SANDBOX: {label: 'Wait for task to start, then browse its sandbox', value: 'SANDBOX'},
    TAIL: {label: 'Wait for task to start, then start tailing:', value: 'TAIL'}
  };

  defaultCommandLineArgs() {
    return Utils.maybe(this.props.task, ['taskRequest', 'pendingTask', 'cmdLineArgsList']);
  }

  show() {
    this.refs.runNowModal.show();
  }

  handleRunNow(dataFromForm) {
    const data = Utils.deepClone(dataFromForm);
    localStorage.setItem(LOCAL_STORAGE_AFTER_TRIGGER_VALUE, data.afterTrigger);

    const runId = uuid.v4();
    data.runId = runId;

    if (data.launchDelay !== undefined) {
      const runAt = (+new Date()) + data.launchDelay;
      data.runAt = runAt;
    }

    if (data.afterTrigger === RunNowModal.AFTER_TRIGGER.TAIL.value) localStorage.setItem(LOCAL_STORAGE_TAIL_AFTER_TRIGGER_FILENAME, data.fileToTail);
    this.props.runNow(data).then((response) => {
      let requestFetchResponse = response || {};
      if (_.isArray(response) && response.length > 0) {
        requestFetchResponse = response[0];
      }
      if (requestFetchResponse.error) {
        Messenger().post({
          message: '<p>This request cannot be run now. This is likely because it is already running.</p>',
          type: 'error'
        });
      } else if (_.contains([RunNowModal.AFTER_TRIGGER.SANDBOX.value, RunNowModal.AFTER_TRIGGER.TAIL.value], data.afterTrigger)) {
        this.refs.taskLauncher.getWrappedInstance().startPolling(
          this.props.requestId,
          runId,
          data.afterTrigger === RunNowModal.AFTER_TRIGGER.TAIL.value && data.fileToTail
        );
      }
    });
  }

  getDefaultFileToTail() {
    const previousFile = localStorage.getItem(LOCAL_STORAGE_TAIL_AFTER_TRIGGER_FILENAME);
    if (previousFile) { return previousFile; }
    if (config.runningTaskLogPath.indexOf('/') === -1) { return config.runningTaskLogPath; }
    return _.rest(config.runningTaskLogPath.split('/'), '1').join('/');
  }

  render() {
    const maybeTaskId = Utils.maybe(this.props.task, ['taskId', 'id']);
    return (
      <span>
        <TaskLauncher
          ref="taskLauncher"
          router={this.props.router}
        />
        <FormModal
          name={this.props.rerun ? 'Rerun this task now' : 'Run a task for this request now'}
          ref="runNowModal"
          action={<span><Glyphicon glyph="flash" /> {this.props.rerun ? 'Rerun' : 'Run'} Task</span>}
          onConfirm={(data) => this.handleRunNow(data)}
          buttonStyle="primary"
          formElements={[
            {
              name: 'launchDelay',
              type: FormModal.INPUT_TYPES.DURATION,
              label: 'Task launch delay: (optional)'
            },
            {
              name: 'commandLineArgs',
              type: FormModal.INPUT_TYPES.MULTIINPUT,
              label: 'Additional command line arguments: (optional)',
              defaultValue: this.defaultCommandLineArgs(),
              valueOptions: this.props.requestArgHistory
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
              defaultValue: localStorage.getItem(LOCAL_STORAGE_AFTER_TRIGGER_VALUE) || RunNowModal.AFTER_TRIGGER.SANDBOX.value
            },
            {
              name: 'fileToTail',
              type: FormModal.INPUT_TYPES.STRING,
              defaultValue: this.getDefaultFileToTail()
            }
          ]}>
          <span>
            <p>Are you sure you want to {this.props.rerun ? 'rerun this task' : 'launch a task for this request'}?</p>
            <pre>{this.props.rerun && maybeTaskId || this.props.requestId}</pre>
          </span>
        </FormModal>
      </span>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  runNow: (data) => dispatch(RunRequest.trigger(ownProps.requestId, data)).then(response => (ownProps.then && ownProps.then(response))),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RunNowModal);
