import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';
import PriorityModal from './PriorityModal';


const tooltip = (
  <ToolTip id="priority">
    Priority
  </ToolTip>
);

export default class PriorityButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    current: PropTypes.number,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-priority-overlay" overlay={tooltip}>
        <a title="Priority">
          <Glyphicon glyph="signal" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <PriorityModal
          ref="modal"
          requestId={this.props.requestId}
          current={this.props.current}
          then={this.props.then}
        />
      </span>
    );
  }
}
