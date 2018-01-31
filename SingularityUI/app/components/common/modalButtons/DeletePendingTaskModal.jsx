import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { DeletePendingOnDemandTask } from '../../../actions/api/tasks';

import ConfirmationDialog from '../../common/ConfirmationDialog';

class DeletePendingTaskModal extends Component {
  static propTypes = {
    taskId: PropTypes.string.isRequired,
    requestType: PropTypes.string.isRequired,
    deletePendingTask: PropTypes.func.isRequired,
    then: PropTypes.func
  };

  show() {
    this.refs.deletePendingTaskModal.show();
  }

  render() {
    return (
      <ConfirmationDialog
        name="Delete Pending Task"
        ref="deletePendingTaskModal"
        action="Delete Pending Task"
        onConfirm={() => this.props.deletePendingTask()}
        buttonStyle="primary">
        <p>Are you sure you want to delete this task?</p>
        <pre>{this.props.taskId}</pre>
      </ConfirmationDialog>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  deletePendingTask: () => dispatch(DeletePendingOnDemandTask.trigger(ownProps.taskId)).then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(DeletePendingTaskModal);
