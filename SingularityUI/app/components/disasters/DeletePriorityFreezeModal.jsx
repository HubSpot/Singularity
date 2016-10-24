import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { FetchPriorityFreeze, DeletePriorityFreeze } from '../../actions/api/disasters';
import FormModal from '../common/modal/FormModal';

class DeletePriorityFreezeModal extends Component {
  static propTypes = {
    user: PropTypes.string,
    action: PropTypes.string.isRequired
  };

  show() {
    this.refs.deletePriorityFreezeModal.show();
  }

  render() {
    return (
      <FormModal
        ref="deletePriorityFreezeModal"
        name="Remove Priority Freeze"
        action="Remove"
        buttonStyle="default"
        onConfirm={(data) => this.props.deletePriorityFreeze()}
        formElements={[]}
      >
        <p>Are you sure you want to remove the current priority freeze?</p>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  deletePriorityFreeze: () => dispatch(DeletePriorityFreeze.trigger()).then(() => {dispatch(FetchPriorityFreeze.trigger([404]));}),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(DeletePriorityFreezeModal);
