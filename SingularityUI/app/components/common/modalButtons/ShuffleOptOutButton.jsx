import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { Button } from 'react-bootstrap';

import { getClickComponent } from '../modal/ModalWrapper';
import ShuffleOptOutModal from './ShuffleOptOutModal';


class ShuffleOptOutButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    isOptedOut: PropTypes.bool.isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  renderButton() {
    if (this.props.isOptedOut) {
      return <Button bsStyle="primary">Enable Shuffling</Button>
    }

    return (
      <Button bsStyle="primary">Disable Shuffling</Button>
    );
  }

  render() {
    return (
      <span>
        {getClickComponent(this.renderButton())}
        <ShuffleOptOutModal
          ref="modal"
          requestId={this.props.requestId}
          isOptedOut={this.props.isOptedOut}
          then={this.props.then}
        />
      </span>
    );
  }
}

function mapStateToProps(state, ownProps) {
  return {
    isOptedOut: state.api.shuffleOptOut[ownProps.requestId],
  };
}

export default connect(mapStateToProps, null)(ShuffleOptOutButton);

