import React, { Component, PropTypes } from 'react';

import { Glyphicon, Modal } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import SimplestModal from '../common/modal/SimplestModal';

import TaskLessTerminal from './TaskLessTerminal';

export default class TaskLessButton extends Component {

  static propTypes = {
    host: PropTypes.string.isRequired,
    task: PropTypes.string.isRequired,
    file: PropTypes.shape({
      isDirectory: PropTypes.bool,
      isTailable: PropTypes.bool,
      name: PropTypes.string,
      downloadLink: PropTypes.string,
      size: PropTypes.number,
      mtime: PropTypes.number
    }).isRequired,

    children: PropTypes.node,
    then: PropTypes.func
  };

  renderTooltip() {
    return (
      <ToolTip id="less">
        Less {this.props.file.name}
      </ToolTip>
    );
  }

  render() {
    return (
      <span>
        <OverlayTrigger placement="left" overlay={this.renderTooltip()} onClick={() => { this.refs.modal.show() }}>
          <a>
            <Glyphicon glyph="fire" />
          </a>
        </OverlayTrigger>
        <SimplestModal ref="modal" bsSize="lg">
          <Modal.Body style={{ padding: 0 }}>
            <TaskLessTerminal
              host={this.props.host}
              port={4141}
              task={this.props.task}
              path={this.props.file.fullPath}
              onClose={() => { this.refs.modal.hide() }}
            />
          </Modal.Body>
        </SimplestModal>
      </span>
    );
  }
}
