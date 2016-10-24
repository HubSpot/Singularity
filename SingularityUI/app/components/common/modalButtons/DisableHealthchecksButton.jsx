import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../modal/ModalWrapper';

import DisableHealthchecksModal from './DisableHealthchecksModal';

export default class DisableHealthchecksButton extends Component {

  static propTypes = {
    requestId: PropTypes.oneOfType([PropTypes.string, PropTypes.array]).isRequired,
    children: PropTypes.node,
    then: PropTypes.func
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
          then={this.props.then}
        />
      </span>
    );
  }
}
