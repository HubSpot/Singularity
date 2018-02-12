import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { BounceRequest } from '../../../actions/api/requests';

import FormModal from '../modal/FormModal';

class BounceModal extends Component {
  static propTypes = {
    requestId: PropTypes.oneOfType([PropTypes.string, PropTypes.array]).isRequired,
    bounceRequest: PropTypes.func.isRequired
  };

  componentDidMount() {
    if (window.location.search.includes('bounce=true')) {
      this.show();
    }
  }

  show() {
    this.refs.bouceModal.show();
  }

  confirm(data) {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
    for (const requestId of requestIds) {
      this.props.bounceRequest(requestId, data);
    }
  }

  static INCREMENTAL_BOUNCE_VALUE = {
    INCREMENTAL: {
      label: 'Kill old tasks as new tasks become healthy',
      value: true
    },
    ALL: {
      label: 'Kill old tasks once ALL new tasks are healthy',
      value: false
    }
  };

  render() {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
    let formElements = [
      {
        name: 'incremental',
        type: FormModal.INPUT_TYPES.RADIO,
        values: _.values(BounceModal.INCREMENTAL_BOUNCE_VALUE),
        defaultValue: BounceModal.INCREMENTAL_BOUNCE_VALUE.INCREMENTAL.value
      },
      {
        name: 'skipHealthchecks',
        type: FormModal.INPUT_TYPES.BOOLEAN,
        label: 'Skip healthchecks during bounce'
      }
    ]

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

    formElements.push(
      {
        name: 'durationMillis',
        type: FormModal.INPUT_TYPES.DURATION,
        label: 'Expiration (optional)',
        help: (
          <div>
            <p>If an expiration duration is specified, this bounce will be aborted if not finished.</p>
            <p>Default value {config.defaultBounceExpirationMinutes} minutes</p>
          </div>
        )
      },
      {
        name: 'message',
        type: FormModal.INPUT_TYPES.STRING,
        label: 'Message (optional)'
      }
    );

    return (
      <FormModal
        name="Bounce Request"
        ref="bouceModal"
        action="Bounce Request"
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
        <p>Are you sure you want to bounce {requestIds.length > 1 ? 'these' : 'this'} request{requestIds.length > 1 && 's'}?</p>
        <pre>{requestIds.join('\n')}</pre>
        <p>Bouncing a request will cause replacement tasks to be scheduled and (under normal conditions) executed immediately.</p>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  bounceRequest: (requestId, data) => dispatch(BounceRequest.trigger(requestId, data)).then(response => (ownProps.then && ownProps.then(response))),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(BounceModal);
