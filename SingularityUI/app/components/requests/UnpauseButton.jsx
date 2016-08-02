import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../common/modal/ModalWrapper';

import UnpauseModal from './UnpauseModal';

const unpauseTooltip = (
  <ToolTip id="unpause">
    Unpause
  </ToolTip>
);

export default class UnpauseButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    children: PropTypes.node
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-unpause-overlay" overlay={unpauseTooltip}>
        <a title="Unpause">
          <Glyphicon glyph="play" />
        </a>
      </OverlayTrigger>
    )
  }

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <UnpauseModal ref="modal" requestId={this.props.requestId} />
      </span>
    );
  }
}
