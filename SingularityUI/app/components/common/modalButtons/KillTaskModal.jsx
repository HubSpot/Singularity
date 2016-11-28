import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { KillTask } from '../../../actions/api/tasks';

import FormModal from '../modal/FormModal';

class KillTaskModal extends Component {
  static propTypes = {
    taskId: PropTypes.string.isRequired,
    shouldShowWaitForReplacementTask: PropTypes.bool,
    killTask: PropTypes.func.isRequired,
    destroy: PropTypes.bool,
    name: PropTypes.string,
    then: PropTypes.func
  };

  constructor() {
    super();

    this.show = this.show.bind(this);
  }

  static defaultProps = {
    name: 'Kill Task'
  };

  show() {
    this.refs.confirmKillTask.show();
  }

  render() {
    let formElements = [];
    if (this.props.shouldShowWaitForReplacementTask) {
      formElements = [{
        name: 'waitForReplacementTask',
        type: FormModal.INPUT_TYPES.BOOLEAN,
        label: 'Wait for replacement task to start before killing task',
        defaultValue: true
      }];
    }

    if (config.shellCommands.length > 0) {
      formElements.push(
        {
        name: 'runShellCommand',
        type: FormModal.INPUT_TYPES.BOOLEAN,
        label: 'Run shell command before killing tasks',
        defaultValue: false
        },
        {
          name: 'runShellCommandBeforeKill',
          type: FormModal.INPUT_TYPES.SELECT,
          dependsOn: 'runShellCommand',
          defaultValue: config.shellCommands[0].name,
          options: config.shellCommands.map((shellCommand) => ({
            label: shellCommand.name,
            value: shellCommand.name
          }))
        }
      );
    }

    formElements.push({
      name: 'message',
      type: FormModal.INPUT_TYPES.STRING,
      label: 'Message (optional)'
    });
    return (
      <FormModal
        name={this.props.name}
        ref="confirmKillTask"
        action={this.props.name}
        onConfirm={(data) => {
          if (data.runShellCommand) {
            data.runShellCommandBeforeKill = {name: data.runShellCommandBeforeKill};
          } else {
            delete data.runShellCommandBeforeKill;
          }
          if (this.props.destroy) {
            data.override = true;
          }
          this.props.killTask(data);
        }}
        buttonStyle="danger"
        formElements={formElements}>
        <span>
          <p>Are you sure you want to kill {this.props.destroy ? '-9' : ''} this task?</p>
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
  killTask: (data) => dispatch(KillTask.trigger(ownProps.taskId, data)).then(() => ownProps.then && ownProps.then())
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(KillTaskModal);
