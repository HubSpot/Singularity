import React, { Component, PropTypes } from 'react';

import FormModal from '../common/FormModal';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default class ScaleButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    scaleAction: PropTypes.func.isRequired,
    currentInstances: PropTypes.number
  };

  render() {
    return (
      <span>
        <a onClick={() => this.refs.unpauseModal.show()}><Glyphicon iconClass="signal" /></a>
        <FormModal
          ref="unpauseModal"
          action="Scale Request"
          onConfirm={(data) => this.props.unpauseAction(this.props.requestId, data)}
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
