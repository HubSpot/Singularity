import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { FetchTaskCredits, DisableTaskCredits } from '../../actions/api/disasters';
import FormModal from '../common/modal/FormModal';

import Utils from '../../utils';

class AddTaskCreditsModal extends Component {
  static propTypes = {
    user: PropTypes.string,
    disableTaskCredits: PropTypes.func.isRequired
  };

  show() {
    this.refs.disableTaskCredits.show();
  }

  render() {
    return (
      <FormModal
        ref="disableTaskCredits"
        name="Disable Task Credits"
        action="Disable Task Credits"
        buttonStyle="info"
        onConfirm={(data) => this.props.disableTaskCredits()}
        formElements={[]}
      />
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  disableTaskCredits: () => dispatch(DisableTaskCredits.trigger()).then(() => {dispatch(FetchTaskCredits.trigger());}),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(AddTaskCreditsModal);