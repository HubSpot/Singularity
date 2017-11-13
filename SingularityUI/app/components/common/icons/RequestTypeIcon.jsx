import React, { Component, PropTypes } from 'react';

import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import Utils from '../../../utils';

export default class RequestTypeIcon extends Component {
  static propTypes = {
    requestType: PropTypes.string,
    translucent: PropTypes.bool
  }

  render() {
    let classNames = `request-type-icon request-type-icon-${this.props.requestType}`;
    if (this.props.translucent) {
      classNames = classNames + " request-type-icon-translucent";
    }
    const tooltip = (
      <ToolTip id="view-request-type">
        {Utils.humanizeText(this.props.requestType)}
      </ToolTip>
    );
    return (
      <OverlayTrigger  placement="top" id="view-request-type-overlay" overlay={tooltip}>
        <span className={classNames} />
      </OverlayTrigger>
    );
  }
}