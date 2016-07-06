import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../common/modal/ModalWrapper';

import PauseModal from './PauseModal';

export default class PauseButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    isScheduled: PropTypes.bool.isRequired,
    children: PropTypes.node
  };

  static defaultProps = {
    children: <a><Glyphicon glyph="play" /></a>
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <PauseModal
          ref="modal"
          requestId={this.props.requestId}
          isScheduled={this.props.isScheduled}
        />
      </span>
    );
  }
}
