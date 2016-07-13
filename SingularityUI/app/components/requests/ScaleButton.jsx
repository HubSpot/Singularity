import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../common/modal/ModalWrapper';

import ScaleModal from './ScaleModal';

export default class ScaleButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    currentInstances: PropTypes.number,
    children: PropTypes.node
  };

  static defaultProps = {
    children: <a><Glyphicon glyph="signal" /></a>
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <ScaleModal
          ref="modal"
          requestId={this.props.requestId}
          currentInstances={this.props.currentInstances}
        />
      </span>
    );
  }
}
