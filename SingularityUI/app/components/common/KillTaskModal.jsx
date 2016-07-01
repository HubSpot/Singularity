import React from 'react';

import FormModal from './FormModal';

export default class KillTaskModal extends React.Component {

  constructor() {
    super();
    this.state = {
      taskId: null
    }
  }

  show(taskId) {
    this.setState({
      taskId: taskId
    });
    this.refs.confirmKillTask.show();
  }

  render() {
    return (
      <FormModal
        ref="confirmKillTask"
        action="Kill Task"
        onConfirm={(data) => this.props.onTaskKill(this.state.taskId, data)}
        buttonStyle="danger"
        formElements={[
          {
            name: 'waitForReplacementTask',
            type: FormModal.INPUT_TYPES.BOOLEAN,
            label: 'Wait for replacement task to start before killing task',
            defaultValue: true
          },
          {
            name: 'message',
            type: FormModal.INPUT_TYPES.STRING,
            label: 'Message (optional)'
          }
        ]}>
        <span>
          <p>Are you sure you want to kill this task?</p>
          <pre>{this.state.taskId}</pre>
          <p>
              Long running process will be started again instantly, scheduled
              tasks will behave as if the task failed and may be rescheduled
              to run in the future depending on whether or not the request
              has <code>numRetriesOnFailure</code> set.
          </p>
        </span>
      </FormModal>
    );
  }
}
