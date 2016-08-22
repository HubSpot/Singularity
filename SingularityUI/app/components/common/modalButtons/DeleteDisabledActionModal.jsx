import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { DeleteDisabledAction, FetchDisabledActions } from '../../../actions/api/disabledActions';

import FormModal from '../modal/FormModal';

class DeleteDisabledActionModal extends Component {
  static propTypes = {
    disabledAction: PropTypes.shape({
      id: PropTypes.string.isRequired,
      type: PropTypes.string.isRequired,
      uri: PropTypes.string.isRequired
    }).isRequired,
    deleteDisabledAction: PropTypes.func.isRequired
  };

  show() {
    this.refs.deleteModal.show();
  }

  render() {
    return (
      <FormModal
        ref="deleteModal"
        name="Delete Disabled Action"
        action="Delete Disabled Action"
        onConfirm={this.props.deleteDisabledAction}
        buttonStyle="danger"
        formElements={[]}>
        <div>
          <pre>
            {this.props.disabledAction.type}
          </pre>
          <p>
            Are you sure you want to delete this disabled action?
          </p>
        </div>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  deleteDisabledAction: () => dispatch(DeleteDisabledAction.trigger(ownProps.disabledAction.type)).then(() => dispatch(FetchDisabledActions.trigger())),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(DeleteDisabledActionModal);
