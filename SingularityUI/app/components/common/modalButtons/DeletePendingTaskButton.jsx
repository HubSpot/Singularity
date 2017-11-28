import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import DeletePendingTaskModal from './DeletePendingTaskModal';

const deletePendingTaskTooltip = (
  <ToolTip id="delete">
    Delete this pending task
  </ToolTip>
);

export default class DeletePendingTaskButton extends Component {

  static propTypes = {
    taskId: PropTypes.string.isRequired,
    requestType: PropTypes.string.isRequired,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-delete-pending-task-overlay" overlay={deletePendingTaskTooltip}>
        <a>
          <Glyphicon glyph="trash" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <DeletePendingTaskModal
          ref="modal"
          taskId={this.props.taskId}
          requestType={this.props.requestType}
          then={this.props.then}
        />
      </span>
    );
  }
}
