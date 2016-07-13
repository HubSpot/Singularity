import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../common/modal/ModalWrapper';

import RemoveModal from './RemoveModal';

export default class RemoveButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    children: PropTypes.node
  };

  static defaultProps = {
    children: <a data-action="remove"><Glyphicon glyph="trash" /></a>
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <RemoveModal ref="modal" requestId={this.props.requestId} />
      </span>
    );
  }
}
