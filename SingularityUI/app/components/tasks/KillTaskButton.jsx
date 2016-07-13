import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../common/modal/ModalWrapper';

import KillTaskModal from './KillTaskModal';

export default class KillTaskButton extends Component {
  static propTypes = {
    taskId: PropTypes.string.isRequired,
    children: PropTypes.node
  };

  static defaultProps = {
    children: <a><Glyphicon glyph="remove" /></a>
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
