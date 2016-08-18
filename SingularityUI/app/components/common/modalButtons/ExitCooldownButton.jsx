import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import ExitCooldownModal from './ExitCooldownModal';

const exitCooldownTooltip = (
  <ToolTip id="exit-cooldown">
    Exit Cooldown
  </ToolTip>
);

export default class ExitCooldownButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: <OverlayTrigger placement="top" id="view-exit-cooldown-overlay" overlay={exitCooldownTooltip}>
        <a>
          <Glyphicon glyph="ice-lolly-tasted" />
        </a>
      </OverlayTrigger>
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <ExitCooldownModal
          ref="modal"
          requestId={this.props.requestId}
          then={this.props.then}
        />
      </span>
    );
  }
}
