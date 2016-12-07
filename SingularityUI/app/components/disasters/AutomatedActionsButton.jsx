import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import AutomatedActionsModal from './AutomatedActionsModal';

const automatedActionsTooltip = (
  <ToolTip id="automated-actions">
    Toggle Automated Actions
  </ToolTip>
);

const AutomatedActionsButton = ({children, user, action}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <AutomatedActionsModal ref={(modal) => {clickComponentData.refs = {modal};}} user={user} action={action} />
    </span>
  );
};

AutomatedActionsButton.propTypes = {
  children: PropTypes.node,
  user: PropTypes.string,
  action: PropTypes.string.isRequired
};

AutomatedActionsButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={automatedActionsTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default AutomatedActionsButton;
