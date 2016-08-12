import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import KillTaskModal from './KillTaskModal';

const killTooltip = (
  <ToolTip id="kill">
    Kill Task
  </ToolTip>
);

export default class KillTaskButton extends Component {
  static propTypes = {
    taskId: PropTypes.string.isRequired,
    children: PropTypes.node
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-kill-overlay" overlay={killTooltip}>
        <a>
          <Glyphicon glyph="remove" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <KillTaskModal ref="modal" taskId={this.props.taskId} />
      </span>
    );
  }
}
