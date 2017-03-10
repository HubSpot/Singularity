import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import EditPriorityFreezeModal from './EditPriorityFreezeModal';

const editFreezeTooltip = (
  <ToolTip id="edit-freeze">
    Edit current priority freeze
  </ToolTip>
);

const EditPriorityFreezeButton = ({children, user, freeze}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <EditPriorityFreezeModal
        ref={(modal) => {clickComponentData.refs = {modal};}}
        user={user}
        freeze={freeze}
      />
    </span>
  );
};

EditPriorityFreezeButton.propTypes = {
  children: PropTypes.node,
  user: PropTypes.string,
  freeze: PropTypes.object,
};

EditPriorityFreezeButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={editFreezeTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default EditPriorityFreezeButton;
