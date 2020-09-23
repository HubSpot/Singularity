import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import CustomizeAgentsTableModal from './CustomizeAgentsTableModal';

const customizeTableTooltip = (
  <ToolTip id="customize-table">
    Customize columns to show in the agents table
  </ToolTip>
);

const CustomizeAgentsTableButton = ({children, columns, paginated, availableAttributes, availableResources}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <CustomizeAgentsTableModal 
        ref={(modal) => {clickComponentData.refs = {modal};}}
        columns={columns}
        paginated={paginated}
        availableAttributes={availableAttributes}
        availableResources={availableResources}
      />
    </span>
  );
};

CustomizeAgentsTableButton.propTypes = {
  children: PropTypes.node,
  columns: PropTypes.object.isRequired,
  paginated: PropTypes.bool.isRequired,
  availableAttributes: PropTypes.arrayOf(PropTypes.string).isRequired,
  availableResources: PropTypes.arrayOf(PropTypes.string).isRequired
};

CustomizeAgentsTableButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={customizeTableTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default CustomizeAgentsTableButton;
