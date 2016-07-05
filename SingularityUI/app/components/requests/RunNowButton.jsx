import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import RunNowModal from './RunNowModal';

export default class RunNowButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired
  };

  render() {
    return (
      <span>
        <a onClick={() => this.refs.runModal.getWrappedInstance().show()}><Glyphicon glyph="flash" /></a>
        <RunNowModal
          ref="runModal"
          requestId={this.props.requestId}
        />
      </span>
    );
  }
}
