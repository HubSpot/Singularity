import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { UnpauseRequest } from '../../../actions/api/requests';

import FormModal from '../modal/FormModal';

class UnpauseModal extends Component {
  static propTypes = {
    requestId: PropTypes.oneOfType([PropTypes.string, PropTypes.array]).isRequired,
    unpauseRequest: PropTypes.func.isRequired
  };

  show() {
    this.refs.unpauseModal.show();
  }

  confirm(data) {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
    for (const requestId of requestIds) {
      this.props.unpauseRequest(requestId, data, [409]);
    }
  }

  render() {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
    return (
      <FormModal
        ref="unpauseModal"
        action="Unpause Request"
        onConfirm={(data) => this.confirm(data)}
        buttonStyle="primary"
        formElements={[
          {
            name: 'skipHealthchecks',
            type: FormModal.INPUT_TYPES.BOOLEAN,
            label: 'Skip healthchecks'
          },
          {
            name: 'message',
            type: FormModal.INPUT_TYPES.STRING,
            label: 'Message (optional)'
          }
        ]}>
        <p>Are you sure you want to unpause {requestIds.length > 1 ? 'these' : 'this'} request{requestIds.length > 1 && 's'}?</p>
        <pre>{requestIds.join('\n')}</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  unpauseRequest: (requestId, data, catchStatusCodes) => dispatch(UnpauseRequest.trigger(requestId, data, catchStatusCodes)).then((response) => ownProps.then && ownProps.then(response)),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(UnpauseModal);
