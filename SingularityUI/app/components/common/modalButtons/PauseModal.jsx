import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { PauseRequest } from '../../../actions/api/requests';

import FormModal from '../modal/FormModal';

class PauseModal extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    isScheduled: PropTypes.bool.isRequired,
    pauseRequest: PropTypes.func.isRequired,
    then: PropTypes.func
  };

  show() {
    this.refs.pauseModal.show();
  }

  render() {
    let formElements = [
      {
        name: 'durationMillis',
        type: FormModal.INPUT_TYPES.DURATION,
        label: 'Expiration (optional)'
      },
      {
        name: 'message',
        type: FormModal.INPUT_TYPES.STRING,
        label: 'Message (optional)'
      }
    ];

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

    if (this.props.isScheduled) {
      formElements = [
        {
          name: 'killTasks',
          type: FormModal.INPUT_TYPES.BOOLEAN,
          label: 'Allow currently executing tasks to finish'
        },
        ...formElements
      ];
    }

    return (
      <FormModal
        name="Pause Request"
        ref="pauseModal"
        action="Pause Request"
        onConfirm={(data) => {
          if (data.runShellCommand) {
            data.runShellCommandBeforeKill = {name: data.runShellCommandBeforeKill};
          } else {
            delete data.runShellCommandBeforeKill;
          }
          this.props.pauseRequest(data)
        }}
        buttonStyle="primary"
        formElements={formElements}>
        <p>Are you sure you want to pause this request?</p>
        <pre>{this.props.requestId}</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  pauseRequest: (data) => dispatch(PauseRequest.trigger(ownProps.requestId, data)).then((response) => ownProps.then && ownProps.then(response)),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(PauseModal);
