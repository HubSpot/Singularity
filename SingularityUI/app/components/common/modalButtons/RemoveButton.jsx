import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import RemoveModal from './RemoveModal';

const removeTooltip = (
  <ToolTip id="remove">
    Remove Request
  </ToolTip>
);

export default class RemoveButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    loadBalanced: PropTypes.bool,
    loadBalancerData: PropTypes.object,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-remove-overlay" overlay={removeTooltip}>
        <a data-action="remove">
          <Glyphicon glyph="trash" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <RemoveModal
          ref="modal"
          requestId={this.props.requestId}
          loadBalanced={this.props.loadBalanced}
          loadBalancerData={this.props.loadBalancerData}
          then={this.props.then}
        />
      </span>
    );
  }
}
