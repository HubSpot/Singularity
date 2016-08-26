import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import NewDisabledActionModal from './NewDisabledActionModal';

const newDisabledActionTooltip = (
  <ToolTip id="new-disabled-action">
    New Disabled Action
  </ToolTip>
);

const NewDisabledActionButton = ({children, user}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <NewDisabledActionModal ref={(modal) => {clickComponentData.refs = {modal};}} user={user} />
    </span>
  );
};

NewDisabledActionButton.propTypes = {
  children: PropTypes.node,
  user: PropTypes.string
};

NewDisabledActionButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={newDisabledActionTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default NewDisabledActionButton;
