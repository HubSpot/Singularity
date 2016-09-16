import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import DeletePriorityFreezeModal from './DeletePriorityFreezeModal';

const DeletePriorityFreezeButton = ({children, user}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <DeletePriorityFreezeModal ref={(modal) => {clickComponentData.refs = {modal};}} user={user} />
    </span>
  );
};

DeletePriorityFreezeButton.propTypes = {
  children: PropTypes.node,
  user: PropTypes.string,
  action: PropTypes.string.isRequired
};

DeletePriorityFreezeButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay">
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default DeletePriorityFreezeButton;