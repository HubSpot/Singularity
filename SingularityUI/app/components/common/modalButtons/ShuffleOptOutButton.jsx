import React, { Component, PropTypes } from 'react';

import { Button } from 'react-bootstrap';
import { getClickComponent } from '../modal/ModalWrapper';

import ShuffleOptOutModal from './ShuffleOptOutModal';

export default class ShuffleOptOutButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    isOptedOut: PropTypes.bool.isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  renderButton() {
    return (
      <Button bsStyle="primary">Shufflability</Button>
    );
  }

  render() {
    return (
      <span>
        {getClickComponent(this.renderButton())}
        <ShuffleOptOutModal
          ref="modal"
          requestId={this.props.requestId}
          then={this.props.then}
        />
      </span>
    );
  }
}
