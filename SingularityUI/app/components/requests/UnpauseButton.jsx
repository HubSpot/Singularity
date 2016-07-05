import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import UnpauseModal from './UnpauseModal';

export default class UnpauseButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired
  };

  render() {
    return (
      <span>
        <a onClick={() => this.refs.unpauseModal.getWrappedInstance().show()}><Glyphicon glyph="play" /></a>
        <UnpauseModal requestId={this.props.requestId} ref="unpauseModal" />
      </span>
    );
  }
}
