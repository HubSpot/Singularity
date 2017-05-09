import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import ScaleModal from './ScaleModal';

const scaleTooltip = (
  <ToolTip id="scale">
    Scale
  </ToolTip>
);

export default class ScaleButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    bounceAfterScaleDefault: PropTypes.bool.isRequired,
    currentInstances: PropTypes.number,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-scale-overlay" overlay={scaleTooltip}>
        <a title="Scale">
          <Glyphicon glyph="signal" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <ScaleModal
          ref="modal"
          requestId={this.props.requestId}
          currentInstances={this.props.currentInstances}
          then={this.props.then}
          bounceAfterScaleDefault={this.props.bounceAfterScaleDefault}
        />
      </span>
    );
  }
}
