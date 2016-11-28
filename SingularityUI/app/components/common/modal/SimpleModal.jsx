import React, { Component, PropTypes } from 'react';

import { Modal, Button } from 'react-bootstrap';


export default class SimpleModal extends Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
    title: PropTypes.node,
    confirmButtonText: PropTypes.node,
    buttonStyle: PropTypes.string,
    mustFill: PropTypes.bool
  };

  constructor(props) {
    super(props);
    this.state = {visible: false};
    _.bindAll(this, 'hide', 'show');
  }

  static defaultProps = {
    confirmButtonText: 'OK',
    buttonStyle: 'success'
  };

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
      <Modal show={this.state.visible} onHide={this.hide} backdrop={this.props.mustFill ? 'static' : true}>
        {this.props.title && (
          <Modal.Header>
            <Modal.Title>{this.props.title}</Modal.Title>
          </Modal.Header>
        )}
        <Modal.Body>
          {this.props.children}
        </Modal.Body>
        <Modal.Footer>
          <Button bsStyle={this.props.buttonStyle} onClick={this.hide}>{this.props.confirmButtonText}</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}
