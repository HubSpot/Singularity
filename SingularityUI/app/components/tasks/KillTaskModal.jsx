import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { KillTask } from '../../actions/api/tasks';

import FormModal from '../common/FormModal';

class KillTaskModal extends Component {
  static propTypes = {
    taskId: PropTypes.string.isRequired,
    killTask: PropTypes.func.isRequired
  };

  constructor() {
    super();

    this.show = this.show.bind(this);
  }

  show() {
    this.refs.confirmKillTask.show();
  }

  render() {
    return (
      <FormModal
        ref="confirmKillTask"
        action="Kill Task"
        onConfirm={(data) => this.props.killTask(data)}
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
          <pre>{this.props.taskId}</pre>
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

const mapDispatchToProps = (dispatch, ownProps) => ({
  killTask: (data) => dispatch(KillTask.trigger(ownProps.taskId, data))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(KillTaskModal);
