import React, { Component, PropTypes } from 'react';

import FormModal from '../common/FormModal';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default class RemoveButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    removeAction: PropTypes.func.isRequired
  };

  render() {
    return (
      <span>
        <a onClick={() => this.refs.removeModal.show()} data-action="remove"><Glyphicon iconClass="trash" /></a>
        <FormModal
          ref="removeModal"
          action="Remove Request"
          onConfirm={(data) => this.props.removeAction(this.props.requestId, data)}
          buttonStyle="danger"
          formElements={[
            {
              name: 'message',
              type: FormModal.INPUT_TYPES.STRING,
              label: 'Message (optional)'
            }
          ]}>
          <p>Are you sure you want to remove this request?</p>
          <pre>{this.props.requestId}</pre>
          <p>If not paused, removing this request will kill all active and scheduled tasks and tasks for it will not run again unless it is reposted to Singularity.</p>
        </FormModal>
      </span>
    );
  }
}
