import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import BounceModal from './BounceModal';

const bounceTooltip = (
  <ToolTip id="bounce">
    Bounce Request
  </ToolTip>
);

export default class BounceButton extends Component {

  static propTypes = {
    requestId: PropTypes.oneOfType([PropTypes.string, PropTypes.array]).isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={bounceTooltip}>
        <a>
          <Glyphicon glyph="refresh" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <BounceModal
          ref="modal"
          requestId={this.props.requestId}
          then={this.props.then}
        />
      </span>
    );
  }
}
