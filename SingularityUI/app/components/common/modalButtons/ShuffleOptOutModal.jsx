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

  renderRequestText() {
    const requestIds = typeof this.props.requestId === 'string' ? [this.props.requestId] : this.props.requestId;
    if (requestIds.length > 1) {
      return 'these requests';
    } else {
      return 'this request';
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
        <p>{this.renderActionText()} task shuffling due to memory pressure or CPU overusage for {this.renderRequestText()}?</p>
        <p>Note that this will not prevent all restarts of your task. A task may still be bounced or shuffled for events such as host decommissions.</p>
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
