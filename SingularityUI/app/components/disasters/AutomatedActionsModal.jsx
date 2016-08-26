import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { FetchDisabledActions, FetchDisastersData, EnableAutomatedActions, DisableAutomatedActions } from '../../actions/api/disasters';
import FormModal from '../common/modal/FormModal';

class AutomatedActionsModal extends Component {
  static propTypes = {
    user: PropTypes.string,
    action: PropTypes.string.isRequired
  };

  show() {
    this.refs.automatedActionsModal.show();
  }

  render() {
    var apiAction;
    if (this.props.action == "Enable") {
      apiAction = EnableAutomatedActions;
    } else {
      apiAction = DisableAutomatedActions;
    }
    var name = this.props.action + " Automated Actions?";
    return (
      <FormModal
        ref="automatedActionsModal"
        name={name}
        action={this.props.action}
        buttonStyle="default"
        onConfirm={(data) => this.props.updateAutomatedActions(apiAction)}
        formElements={[]}
      >
        <p>Are you sure you want to {this.props.action} automated disaster actions?</p>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  updateAutomatedActions: (apiAction) => dispatch(apiAction.trigger()).then(() => {dispatch(FetchDisastersData.trigger()) && dispatch(FetchDisabledActions.trigger());}),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(AutomatedActionsModal);
