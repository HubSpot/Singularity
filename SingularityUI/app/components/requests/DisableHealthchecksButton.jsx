import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../common/modal/ModalWrapper';

import DisableHealthchecksModal from './DisableHealthchecksModal';

export default class DisableHealthchecksButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    children: PropTypes.node
  };

  static defaultProps = {
    children: <a><Glyphicon glyph="apple" /></a>
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <DisableHealthchecksModal
          ref="modal"
          requestId={this.props.requestId}
        />
      </span>
    );
  }
}
