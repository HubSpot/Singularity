import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { SkipRequestHealthchecks } from '../../../actions/api/requests';

import FormModal from '../modal/FormModal';

class EnableHealthchecksModal extends Component {
  static propTypes = {
    requestId: PropTypes.oneOfType([PropTypes.string, PropTypes.array]).isRequired,
    enableHealthchecks: PropTypes.func.isRequired,
    then: PropTypes.func
  };

  show() {
    this.refs.enableHealthchecksModal.show();
  }

  confirm(data) {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
    for (const requestId of requestIds) {
      this.props.enableHealthchecks(requestId, data);
    }
  }

  render() {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
    return (
      <FormModal
        name="Enable Healthchecks"
        ref="enableHealthchecksModal"
        action="Enable Healthchecks"
        onConfirm={(data) => this.confirm(data)}
        buttonStyle="primary"
        formElements={[
          {
            name: 'durationMillis',
            type: FormModal.INPUT_TYPES.DURATION,
            label: 'Expiration (optional)',
            help: 'If an expiration duration is specified, this action will be reverted afterwards.'
          },
          {
            name: 'message',
            type: FormModal.INPUT_TYPES.STRING,
            label: 'Message (optional)'
          }
        ]}>
        <p>Turn <strong>on</strong> healthchecks for {requestIds.length > 1 ? 'these' : 'this'} request{requestIds.length > 1 && 's'}.</p>
        <pre>{requestIds.join('\n')}</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  enableHealthchecks: (requestId, data) => dispatch(SkipRequestHealthchecks.trigger(
    requestId,
    {...data, skipHealthchecks: false}
  )).then(response => (ownProps.then && ownProps.then(response))),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(EnableHealthchecksModal);
