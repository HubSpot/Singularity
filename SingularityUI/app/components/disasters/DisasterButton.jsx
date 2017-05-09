import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import DisasterModal from './DisasterModal';

const disasterTooltip = (
  <ToolTip id="disaster-activate">
    Activate a new disaster
  </ToolTip>
);

const DisasterButton = ({children, user, action, type}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <DisasterModal ref={(modal) => {clickComponentData.refs = {modal};}} user={user} action={action} type={type} />
    </span>
  );
};

DisasterButton.propTypes = {
  children: PropTypes.node,
  user: PropTypes.string,
  action: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired
};

DisasterButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={disasterTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default DisasterButton;
