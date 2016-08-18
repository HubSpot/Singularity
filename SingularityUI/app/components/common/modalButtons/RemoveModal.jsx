import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { RemoveRequest } from '../../../actions/api/requests';

import FormModal from '../modal/FormModal';

class RemoveModal extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    removeRequest: PropTypes.func.isRequired
  };

  show() {
    this.refs.removeModal.show();
  }

  render() {
    return (
      <FormModal
        name="Remove Request"
        ref="removeModal"
        action="Remove Request"
        onConfirm={this.props.removeRequest}
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
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  removeRequest: (data) => dispatch(RemoveRequest.trigger(ownProps.requestId, data)).then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RemoveModal);
