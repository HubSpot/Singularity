import React, { Component, PropTypes } from 'react';
import { withRouter } from 'react-router';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import RunNowModal from './RunNowModal';
import { getClickComponent } from '../common/modal/ModalWrapper';

const runNowTooltip = (
  <ToolTip id="run-now">
    Run Now
  </ToolTip>
);

class RunNowButton extends Component {

  static propTypes = {
    requestId: PropTypes.string.isRequired,
    children: PropTypes.node,
    router: PropTypes.object
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-run-now-overlay" overlay={runNowTooltip}>
        <a title="Run Now">
          <Glyphicon glyph="flash" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        <span>{getClickComponent(this)}</span>
        <RunNowModal ref="modal" requestId={this.props.requestId} router={this.props.router} />
      </span>
    );
  }
}

export default withRouter(RunNowButton);
