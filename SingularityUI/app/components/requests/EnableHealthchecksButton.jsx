import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../common/modal/ModalWrapper';

import EnableHealthchecksModal from './EnableHealthchecksModal';

export default class EnableHealthchecksButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
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
        <EnableHealthchecksModal
          ref="modal"
          requestId={this.props.requestId}
          then={this.props.then}
        />
      </span>
    );
  }
}
