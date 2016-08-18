import React, { Component, PropTypes } from 'react';

import { Button } from 'react-bootstrap';

import { getClickComponent } from '../modal/ModalWrapper';

import AdvanceDeployModal from './AdvanceDeployModal';

export default class AdvanceDeployButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    deployId: PropTypes.string.isRequired,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <Button bsStyle="primary">
        Advance Deploy
      </Button>
    )
  };

  render() {
    return (
      <span style={{margin: 5}}>
        {getClickComponent(this)}
        <AdvanceDeployModal
          ref="modal"
          deployId={this.props.deployId}
          requestId={this.props.requestId}
          then={this.props.then}
        />
      </span>
    );
  }
}
