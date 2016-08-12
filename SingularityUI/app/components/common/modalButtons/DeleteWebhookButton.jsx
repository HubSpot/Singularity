import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import DeleteWebhookModal from './DeleteWebhookModal';

const deleteWebhookTooltip = (
  <ToolTip id="delete">
    Delete this webhook
  </ToolTip>
);

export default class DeleteWebhookButton extends Component {

  static propTypes = {
    webhook: PropTypes.shape({
      uri: PropTypes.string.isRequired,
      id: PropTypes.string.isRequired,
      type: PropTypes.string.isRequired
    }).isRequired
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={deleteWebhookTooltip}>
        <a>
          <Glyphicon glyph="trash" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <DeleteWebhookModal
          ref="modal"
          webhook={this.props.webhook}
        />
      </span>
    );
  }
}
