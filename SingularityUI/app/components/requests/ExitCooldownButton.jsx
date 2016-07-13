import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../common/modal/ModalWrapper';

import ExitCooldownModal from './ExitCooldownModal';

export default class ExitCooldownButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    children: PropTypes.node
  };

  static defaultProps = {
    children: <a><Glyphicon glyph="ice-lolly-tasted" /></a>
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <ExitCooldownModal
          ref="modal"
          requestId={this.props.requestId}
        />
      </span>
    );
  }
}
