import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import ScaleModal from './ScaleModal';

export default class ScaleButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    currentInstances: PropTypes.number
  };

  render() {
    return (
      <span>
        <a onClick={() => this.refs.unpauseModal.getWrappedInstance().show()}><Glyphicon glyph="signal" /></a>
        <ScaleModal ref="unpauseModal" requestId={this.props.requestId} />
      </span>
    );
  }
}
