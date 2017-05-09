import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import DisableTaskCreditsModal from './DisableTaskCreditsModal';

const disableTaskCreditsTooltip = (
  <ToolTip id="new-task-credits">
    Add/Enable Task Credits
  </ToolTip>
);

const DisableTaskCreditsButton = ({children, user}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <DisableTaskCreditsModal ref={(modal) => {clickComponentData.refs = {modal};}} user={user} />
    </span>
  );
};

DisableTaskCreditsButton.propTypes = {
  children: PropTypes.node,
  user: PropTypes.string
};

DisableTaskCreditsButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={disableTaskCreditsTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default DisableTaskCreditsButton;
