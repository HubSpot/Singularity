import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import RunNowModal from './RunNowModal';
import { getClickComponent } from '../common/modal/ModalWrapper';

export default class RunNowButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    children: PropTypes.node
  };

  static defaultProps = {
    children: <a><Glyphicon glyph="flash" /></a>
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <RunNowModal ref="modal" requestId={this.props.requestId} />
      </span>
    );
  }
}
