import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../common/modal/ModalWrapper';

import BounceModal from './BounceModal';

export default class BounceButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    children: PropTypes.node
  };

  static defaultProps = {
    children: <a><Glyphicon glyph="refresh" /></a>
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <BounceModal
          ref="modal"
          requestId={this.props.requestId}
        />
      </span>
    );
  }
}
