import React, { Component, PropTypes } from 'react';

import FormModal from '../common/FormModal';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default class UnpauseButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    unpauseAction: PropTypes.func.isRequired
  };

  render() {
    return (
      <span>
        <a onClick={() => this.refs.unpauseModal.show()}><Glyphicon iconClass="play" /></a>
        <FormModal
          ref="unpauseModal"
          action="Unpause Request"
          onConfirm={(data) => this.props.unpauseAction(this.props.requestId, data)}
          buttonStyle="primary"
          formElements={[
            {
              name: 'message',
              type: FormModal.INPUT_TYPES.STRING,
              label: 'Message (optional)'
            }
          ]}>
          <p>Are you sure you want to unpause this request?</p>
          <pre>{this.props.requestId}</pre>
        </FormModal>
      </span>
    );
  }
}
