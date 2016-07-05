import React from 'react';

import FormModal from './FormModal';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default class RunNowModal extends React.Component {

  static AFTER_TRIGGER = {
    STAY: {label: 'Stay on this page', value: 'STAY'},
    SANDBOX: {label: 'Wait for task to start, then browse its sandbox', value: 'SANDBOX'},
    TAIL: {label: 'Wait for task to start, then start tailing:', value: 'TAIL'}
  };

  constructor() {
    super();
    this.state = {
      requestId: null
    }
  }

  show(requestId) {
    this.setState({
      requestId: requestId
    });
    this.refs.runNow.show();
  }

  render() {
    return (
      <FormModal
        ref="runNow"
        action={<span><Glyphicon iconClass="flash" /> Run Task</span>}
        onConfirm={(data) => this.props.onRunNow(this.state.requestId, data)}
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
          <pre>{this.state.requestId}</pre>
        </span>
      </FormModal>
    );
  }
}
