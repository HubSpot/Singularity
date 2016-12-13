import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import NewPriorityFreezeModal from './NewPriorityFreezeModal';

const newFreezeTooltip = (
  <ToolTip id="new-freeze">
    Create a new priority freeze
  </ToolTip>
);

const NewPriorityFreezeButton = ({children, user}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <NewPriorityFreezeModal ref={(modal) => {clickComponentData.refs = {modal};}} user={user} />
    </span>
  );
};

NewPriorityFreezeButton.propTypes = {
  children: PropTypes.node,
  user: PropTypes.string
};

NewPriorityFreezeButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={newFreezeTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default NewPriorityFreezeButton;
