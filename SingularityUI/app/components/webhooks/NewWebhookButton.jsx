import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../common/modal/ModalWrapper';

import NewWebhookModal from './NewWebhookModal';

const newWebhookTooltip = (
  <ToolTip id="new-webhook">
    New webhook
  </ToolTip>
);

export default class DeleteWebhookButton extends Component {
  static propTypes = {
    user: PropTypes.string
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={newWebhookTooltip}>
        <a>
          <Glyphicon glyph="plus" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <NewWebhookModal ref="modal" user={this.props.user} />
      </span>
    );
  }
}
