import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { CancelDeploy } from '../../../actions/api/deploys';

import FormModal from '../../common/modal/FormModal';

class CancelDeployModal extends Component {
  static propTypes = {
    deployId: PropTypes.string.isRequired,
    requestId: PropTypes.string.isRequired,
    cancelDeploy: PropTypes.func.isRequired,
    then: PropTypes.func
  };

  show() {
    this.refs.cancelModal.show();
  }

  render() {
    return (
      <FormModal
        name="Cancel Deploy"
        ref="cancelModal"
        action="Cancel Deploy"
        onConfirm={() => this.props.cancelDeploy()}
        buttonStyle="primary"
        formElements={[]}>
        <p>Are you sure you want to cancel this deploy?</p>
        <pre>{this.props.deployId}</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  cancelDeploy: () => dispatch(CancelDeploy.trigger(ownProps.deployId, ownProps.requestId)).then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(CancelDeployModal);
