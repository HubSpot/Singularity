import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { ForceFailover } from '../../actions/api/disasters';
import FormModal from '../common/modal/FormModal';

class ForceFailoverModal extends Component {
  static propTypes = {
    user: PropTypes.string,
    forceFailover: PropTypes.func.isRequired,
  };

  show() {
    this.refs.forceFailoverModal.show();
  }

  render() {
    return (
      <FormModal
        ref="forceFailoverModal"
        name="Force Failover"
        action="Failover"
        buttonStyle="danger"
        onConfirm={(data) => this.props.forceFailover()}
        formElements={[]}
      >
        <p>Are you sure you want to force the leading singularity instance to restart?</p>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch) => ({
  forceFailover: () => dispatch(ForceFailover.trigger()),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(ForceFailoverModal);
