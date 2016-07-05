import React, { Component, PropTypes } from 'react';

import FormModal from '../common/FormModal';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default class ScaleButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    scaleAction: PropTypes.func.isRequired,
    bounceAction: PropTypes.func.isRequired,
    currentInstances: PropTypes.number
  };

  static INCREMENTAL_BOUNCE_VALUE = {
    INCREMENTAL: {label: 'Kill old tasks as new tasks become healthy', value: true},
    ALL: {label: 'Kill old tasks once ALL new tasks are healthy', value: false}
  };

  handleScale(data) {
    console.log(data);
    this.props.scaleAction(this.props.requestId, {instances: data.instances, durationMillis: data.durationMillis, message: data.message}).then((r) => console.log(r));
    if (data.bounce) {
      this.props.bounceAction(this.props.requestId, {incremental: !!data.incremental}).then((r) => console.log(r));
    }
  }

  render() {
    return (
      <span>
        <a onClick={() => this.refs.unpauseModal.show()}><Glyphicon iconClass="signal" /></a>
        <FormModal
          ref="unpauseModal"
          action="Scale Request"
          onConfirm={(data) => this.handleScale(data)}
          buttonStyle="primary"
          formElements={[
            {
              name: 'instances',
              min: 1,
              type: FormModal.INPUT_TYPES.NUMBER,
              label: 'Number of instances:',
              defaultValue: this.props.currentInstances,
              isRequired: true
            },
            {
              name: 'bounce',
              type: FormModal.INPUT_TYPES.BOOLEAN,
              label: 'Bounce after scaling',
              defaultValue: false
            },
            {
              name: 'incremental',
              type: FormModal.INPUT_TYPES.RADIO,
              values: _.values(ScaleButton.INCREMENTAL_BOUNCE_VALUE),
              dependsOn: 'bounce',
              defaultValue: ScaleButton.INCREMENTAL_BOUNCE_VALUE.INCREMENTAL.value
            },
            {
              name: 'durationMillis',
              type: FormModal.INPUT_TYPES.DURATION,
              label: 'Expiration: (optional)'
            },
            {
              name: 'message',
              type: FormModal.INPUT_TYPES.STRING,
              label: 'Message: (optional)'
            }
          ]}>
          <p>Scaling request:</p>
          <pre>{this.props.requestId}</pre>
        </FormModal>
      </span>
    );
  }
}
