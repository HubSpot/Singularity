import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import ForceFailoverModal from './ForceFailoverModal';

const forceFailoverTooltip = (
  <ToolTip id="edit-freeze">
    Force the leading Singularity instance to restart
  </ToolTip>
);

const ForceFailoverButton = ({children, user, freeze}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <ForceFailoverModal
        ref={(modal) => {clickComponentData.refs = {modal};}}
      />
    </span>
  );
};

ForceFailoverButton.propTypes = {
  children: PropTypes.node,
};

ForceFailoverButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={forceFailoverTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default ForceFailoverButton;
