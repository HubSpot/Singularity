import React, { Component, PropTypes } from 'react';

import { Modal } from 'react-bootstrap';

export default class SimplestModal extends Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
  };

  constructor(props) {
    super(props);
    this.state = {visible: false};
    _.bindAll(this, 'hide', 'show');
  }
  
  hide() {
    this.setState({
      visible: false
    });
  }

  show() {
    this.setState({
      visible: true
    });
  }

  render() {
    return (
      <Modal show={this.state.visible} onHide={this.hide} {...this.props} />
    );
  }
}
