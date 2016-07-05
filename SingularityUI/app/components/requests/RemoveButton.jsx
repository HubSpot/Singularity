import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';

import RemoveModal from './RemoveModal';

export default class RemoveButton extends Component {

  static propTypes = {
    children: PropTypes.node,
    requestId: PropTypes.string.isRequired,
    className: PropTypes.string
  };

  static defaultProps = {
    children: <Glyphicon glyph="trash" />
  };

  render() {
    return (
      <span>
        <a className={this.props.className} onClick={() => this.refs.removeModal.getWrappedInstance().show()} data-action="remove">{this.props.children}</a>
        <RemoveModal ref="removeModal" requestId={this.props.requestId} />
      </span>
    );
  }
}
