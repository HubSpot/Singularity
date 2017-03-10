import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { FetchTaskCredits, AddTaskCredits } from '../../actions/api/disasters';
import FormModal from '../common/modal/FormModal';

import Utils from '../../utils';

class AddTaskCreditsModal extends Component {
  static propTypes = {
    user: PropTypes.string,
    addTaskCredits: PropTypes.func.isRequired
  };

  show() {
    this.refs.addTaskCreditsModal.show();
  }

  render() {
    return (
      <FormModal
        ref="addTaskCreditsModal"
        name="Add/Enable Task Credits"
        action="Add/Enable Task Credits"
        buttonStyle="info"
        onConfirm={(data) => this.props.addTaskCredits(data.credits)}
        formElements={[
          {
            type: FormModal.INPUT_TYPES.NUMBER,
            name: 'credits',
            label: 'Credits',
            help: 'When task credits are enabled, each credit added will allow Singularity to launch 1 task. Adding any credits will enable task credits',
            isRequired: true
          }
        ]}
      />
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  addTaskCredits: (credits) => dispatch(AddTaskCredits.trigger(credits)).then(() => {dispatch(FetchTaskCredits.trigger());}),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(AddTaskCreditsModal);