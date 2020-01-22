import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import FormModal from '../modal/FormModal';
import { EnableRequestShuffleOptOut, DisableRequestShuffleOptOut } from '../../../actions/api/requests.es6';

class ShuffleOptOutModal extends Component {

  static propTypes = {
    requestId: PropTypes.oneOfType([PropTypes.string, PropTypes.array]).isRequired,
    isOptedOut: PropTypes.bool.isRequired,
    saveShuffleOptOut: PropTypes.func.isRequired,
    then: PropTypes.func
  };

  show() {
    this.refs.shuffleOptOutModal.show();
  }

  confirm(data) {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
    for (const requestId of requestIds) {
      this.props.saveShuffleOptOut(requestId, data, [409]);
    }
  }

  renderActionText() {
    if (this.props.isOptedOut) {
      return <strong>Enable</strong>
    } else {
      return <strong>Disable</strong>
    }
  }

  render() {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
    const { isOptedOut } = this.props;

    return (
      <FormModal
        name={isOptedOut ? "Enable Shuffling For Request" : "Disable Shuffling For Request"}
        ref="shuffleOptOutModal"
        action={isOptedOut ? "Enable Shuffling" : "Disable Shuffling"}
        onConfirm={(data) => this.confirm(data)}
        buttonStyle="primary"
        formElements={[]}>
        <p>{this.renderActionText()} shuffling for {requestIds.length > 1 ? 'these' : 'this'} request{requestIds.length > 1 && 's'}?</p>
        <p>Note that Singularity may still bounce your tasks if necessary, such as during host failures.</p>
        <pre>{requestIds.join('\n')}</pre>
      </FormModal>
    );
  }
}

function mapDispatchToProps(dispatch, ownProps) {
  return {
    saveShuffleOptOut: (requestId, data) => {
      if (ownProps.isOptedOut) {
        dispatch(DisableRequestShuffleOptOut.trigger(requestId, data)).then((response) => {
          ownProps.then && ownProps.then(response);
        });
      } else {
        dispatch(EnableRequestShuffleOptOut.trigger(requestId, data)).then((response) => {
          ownProps.then && ownProps.then(response);
        });
      }
    },
  };
}

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(ShuffleOptOutModal);
