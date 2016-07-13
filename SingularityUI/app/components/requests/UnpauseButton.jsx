import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../common/modal/ModalWrapper';

import UnpauseModal from './UnpauseModal';

export default class UnpauseButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    children: PropTypes.node
  };

  static defaultProps = {
    children: <a><Glyphicon glyph="play" /></a>
  }

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <UnpauseModal ref="modal" requestId={this.props.requestId} />
      </span>
    );
  }
}
