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
    shouldShowWaitForReplacementTask: PropTypes.bool,
    children: PropTypes.node,
    name: PropTypes.string,
    destroy: PropTypes.bool,
    then: PropTypes.func
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
        <KillTaskModal
          name={this.props.name}
          destroy={this.props.destroy}
          then={this.props.then}
          ref="modal"
          taskId={this.props.taskId}
          shouldShowWaitForReplacementTask={this.props.shouldShowWaitForReplacementTask}
        />
      </span>
    );
  }
}
