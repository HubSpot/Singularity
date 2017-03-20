import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import AddTaskCreditsModal from './AddTaskCreditsModal';

const addTaskCreditsTooltip = (
  <ToolTip id="new-task-credits">
    Add/Enable Task Credits
  </ToolTip>
);

const AddTaskCreditsButton = ({children, user}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <AddTaskCreditsModal ref={(modal) => {clickComponentData.refs = {modal};}} user={user} />
    </span>
  );
};

AddTaskCreditsButton.propTypes = {
  children: PropTypes.node,
  user: PropTypes.string
};

AddTaskCreditsButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={addTaskCreditsTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default AddTaskCreditsButton;
