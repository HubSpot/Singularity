import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import PauseModal from './PauseModal';

const pauseTooltip = (
  <ToolTip id="pause">
    Pause
  </ToolTip>
);

export default class PauseButton extends Component {

  static propTypes = {
    requestId: PropTypes.oneOfType([PropTypes.string, PropTypes.array]).isRequired,
    isScheduled: PropTypes.bool.isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-pause-overlay" overlay={pauseTooltip}>
        <a>
          <Glyphicon glyph="pause" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <PauseModal
          ref="modal"
          requestId={this.props.requestId}
          isScheduled={this.props.isScheduled}
          then={this.props.then}
        />
      </span>
    );
  }
}
