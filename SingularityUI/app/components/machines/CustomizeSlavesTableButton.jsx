import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import CustomizeSlavesTableModal from './CustomizeSlavesTableModal';

const customizeTableTooltip = (
  <ToolTip id="customize-table">
    Customize columns to show in the slaves table
  </ToolTip>
);

const CustomizeSlavesTableButton = ({children, columns, paginated, availableAttributes, availableResources}) => {
  const clickComponentData = {props: {children}};
  return (
    <span>
      {getClickComponent(clickComponentData)}
      <CustomizeSlavesTableModal 
        ref={(modal) => {clickComponentData.refs = {modal};}}
        columns={columns}
        paginated={paginated}
        availableAttributes={availableAttributes}
        availableResources={availableResources}
      />
    </span>
  );
};

CustomizeSlavesTableButton.propTypes = {
  children: PropTypes.node,
  columns: PropTypes.object.isRequired,
  paginated: PropTypes.bool.isRequired,
  availableAttributes: PropTypes.arrayOf(PropTypes.string).isRequired,
  availableResources: PropTypes.arrayOf(PropTypes.string).isRequired
};

CustomizeSlavesTableButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={customizeTableTooltip}>
      <a>
        <Glyphicon glyph="plus" />
      </a>
    </OverlayTrigger>
  )
};

export default CustomizeSlavesTableButton;
