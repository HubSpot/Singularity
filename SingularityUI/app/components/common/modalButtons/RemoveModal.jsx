import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { RemoveRequest } from '../../../actions/api/requests';

import FormModal from '../modal/FormModal';

const controls = (loadBalanced) => {
  const formElements = [];
  formElements.push({
    name: 'message',
    type: FormModal.INPUT_TYPES.STRING,
    label: 'Message (optional)'
  });
  if (loadBalanced) {
    formElements.push({
      name: 'deleteFromLoadBalancer',
      type: FormModal.INPUT_TYPES.BOOLEAN,
      label: 'Remove from load balancer',
      defaultValue: true
    });
  }

  return formElements;
};

class RemoveModal extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    loadBalanced: PropTypes.bool,
    loadBalancerData: PropTypes.object,
    removeRequest: PropTypes.func.isRequired
  };

  show() {
    this.refs.removeModal.show();
  }

  render() {
    const loadBalancerWarning = (
      <div>
        <p>If you remove this request from the load balancer, the following settings will also be removed:</p>
        <pre>{JSON.stringify(this.props.loadBalancerData, null, 2)}</pre>
      </div>
    );

    return (
      <FormModal
        name="Remove Request"
        ref="removeModal"
        action="Remove Request"
        onConfirm={this.props.removeRequest}
        buttonStyle="danger"
        formElements={controls(this.props.loadBalanced)}>
        <p>Are you sure you want to remove this request?</p>
        <pre>{this.props.requestId}</pre>
        <p>If not paused, removing this request will kill all active and scheduled tasks and tasks for it will not run again unless it is reposted to Singularity.</p>
        {!_.isEmpty(this.props.loadBalancerData) && loadBalancerWarning}
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
