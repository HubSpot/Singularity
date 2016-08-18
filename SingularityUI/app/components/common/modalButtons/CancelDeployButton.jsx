import React, { Component, PropTypes } from 'react';

import { Button } from 'react-bootstrap';

import { getClickComponent } from '../modal/ModalWrapper';

import CancelDeployModal from './CancelDeployModal';

export default class CancelDeployButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    deployId: PropTypes.string.isRequired,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <Button bsStyle="warning">
        Cancel Deploy
      </Button>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <CancelDeployModal
          ref="modal"
          deployId={this.props.deployId}
          requestId={this.props.requestId}
          then={this.props.then}
        />
      </span>
    );
  }
}
