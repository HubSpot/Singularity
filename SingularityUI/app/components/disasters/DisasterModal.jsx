import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { FetchDisastersData, FetchDisabledActions, DeleteDisaster, NewDisaster } from '../../actions/api/disasters';
import FormModal from '../common/modal/FormModal';
import Utils from '../../utils';

class DisasterModal extends Component {
  static propTypes = {
    user: PropTypes.string,
    action: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    updateDisaster: PropTypes.func.isRequired
  };

  show() {
    this.refs.disasterModal.show();
  }

  render() {
    var apiAction;
    if (this.props.action == "Activate") {
      apiAction = NewDisaster;
    } else {
      apiAction = DeleteDisaster;
    }
    var name = this.props.action + " " + Utils.humanizeText(this.props.type) + " Disaster?";
    return (
      <FormModal
        ref="disasterModal"
        name={name}
        action={this.props.action}
        buttonStyle="default"
        onConfirm={(data) => this.props.updateDisaster(this.props.type, apiAction)}
        formElements={[]}
      >
        <p>Are you sure you want to activate a {Utils.humanizeText(this.props.type)} disaster?</p>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  updateDisaster: (type, apiAction) => dispatch(apiAction.trigger(type)).then(() => {dispatch(FetchDisastersData.trigger()) && dispatch(FetchDisabledActions.trigger());}),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(DisasterModal);
