import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { UnpauseRequest } from '../../actions/api/requests';

import FormModal from '../common/FormModal';

class UnpauseModal extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    unpauseRequest: PropTypes.func.isRequired
  };

  show() {
    this.refs.unpauseModal.show();
  }

  render() {
    return (
      <FormModal
        ref="unpauseModal"
        action="Unpause Request"
        onConfirm={(data) => this.props.unpauseRequest(data)}
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
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  unpauseRequest: (data) => dispatch(UnpauseRequest.trigger(ownProps.requestId, data)),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(UnpauseModal);
