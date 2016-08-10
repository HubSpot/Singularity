import React, { PropTypes } from 'react';

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

const NewWebhookButton = ({children, user}) => {
  const clickComponentData = {children};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <NewWebhookModal ref={(modal) => {clickComponentData.refs = {modal};}} user={user} />
    </span>
  );
};

NewWebhookButton.propTypes = {
  children: PropTypes.node,
  user: PropTypes.string
};

NewWebhookButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={newWebhookTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default NewWebhookButton;
