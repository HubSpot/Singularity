import React, { Component, PropTypes } from 'react';
import { withRouter } from 'react-router';

import { Glyphicon } from 'react-bootstrap';
import RunNowModal from './RunNowModal';
import { getClickComponent } from '../common/modal/ModalWrapper';

class RunNowButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    children: PropTypes.node,
    router: PropTypes.object
  };

  static defaultProps = {
    children: <a><Glyphicon glyph="flash" /></a>
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <RunNowModal ref="modal" requestId={this.props.requestId} router={this.props.router} />
      </span>
    );
  }
}

export default withRouter(RunNowButton);
