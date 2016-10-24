import React, { Component, PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { getClickComponent } from '../common/modal/ModalWrapper';
import DeleteDisabledActionModal from './DeleteDisabledActionModal';

const deleteDisabledActionTooltip = (
  <ToolTip id="delete">
    Delete this disabled action
  </ToolTip>
);

export default class DeleteDisabledActionButton extends Component {

  static propTypes = {
    disabledAction: PropTypes.shape({
      type: PropTypes.string.isRequired,
      message: PropTypes.string,
      user: PropTypes.string
    }).isRequired
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-bounce-overlay" overlay={deleteDisabledActionTooltip}>
        <a>
          <Glyphicon glyph="trash" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <DeleteDisabledActionModal
          ref="modal"
          disabledAction={this.props.disabledAction}
        />
      </span>
    );
  }
}
