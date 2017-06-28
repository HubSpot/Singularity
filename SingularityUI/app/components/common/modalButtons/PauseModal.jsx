import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { PauseRequest } from '../../../actions/api/requests';

import FormModal from '../modal/FormModal';

class PauseModal extends Component {

  static propTypes = {
    requestId: PropTypes.oneOfType([PropTypes.string, PropTypes.array]).isRequired,
    isScheduled: PropTypes.bool,
    pauseRequest: PropTypes.func.isRequired,
    then: PropTypes.func
  };

  show() {
    this.refs.pauseModal.show();
  }

  confirm(data) {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
    for (const requestId of requestIds) {
      this.props.pauseRequest(requestId, data, [409]);
    }
  }

  render() {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
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
          name: 'allowRunningTasksToFinish',
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
          this.confirm(data)
        }}
        buttonStyle="primary"
        formElements={formElements}>
        <p>Are you sure you want to pause {requestIds.length > 1 ? 'these' : 'this'} request{requestIds.length > 1 && 's'}?</p>
        <pre>{requestIds.join('\n')}</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  pauseRequest: (requestId, data, catchStatusCodes) => dispatch(PauseRequest.trigger(requestId, data, catchStatusCodes)).then((response) => ownProps.then && ownProps.then(response)),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(PauseModal);
