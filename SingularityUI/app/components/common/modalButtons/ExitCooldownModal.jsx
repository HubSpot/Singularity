import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { ExitRequestCooldown } from '../../../actions/api/requests';

import FormModal from '../modal/FormModal';

class ExitCooldownModal extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    exitRequestCooldown: PropTypes.func.isRequired,
    then: PropTypes.func
  };

  show() {
    this.refs.exitCooldownModal.show();
  }

  render() {
    return (
      <FormModal
        name="Exit Request Cooldown"
        ref="exitCooldownModal"
        action="Exit Request Cooldown"
        onConfirm={(data) => this.props.exitRequestCooldown(data)}
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
        <p>Are you sure you want to remove this request from cooldown?</p>
        <pre>{this.props.requestId}</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  exitRequestCooldown: (data) => dispatch(ExitRequestCooldown.trigger(ownProps.requestId, data)).then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(ExitCooldownModal);
