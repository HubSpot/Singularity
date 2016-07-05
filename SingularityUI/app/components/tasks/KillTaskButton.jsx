import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import KillTaskModal from './KillTaskModal';

export default class KillTaskButton extends Component {
  static propTypes = {
    taskId: PropTypes.string.isRequired
  };

  render() {
    return (
      <span>
        <a onClick={() => this.refs.killTaskModal.getWrappedInstance().show()}><Glyphicon glyph="remove" /></a>
        <KillTaskModal
          ref="killTaskModal"
          taskId={this.props.taskId}
        />
      </span>
    );
  }
}
