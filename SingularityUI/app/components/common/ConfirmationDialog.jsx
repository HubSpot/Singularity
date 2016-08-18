import React, { PropTypes } from 'react';

import { Modal, Button } from 'react-bootstrap';

export default class ConfirmationDialog extends React.Component {

  static propTypes = {
    action: React.PropTypes.string.isRequired,
    onConfirm: React.PropTypes.func.isRequired,
    buttonStyle: React.PropTypes.string,
    children: PropTypes.object
  }

  constructor(props) {
    super(props);
    this.state = {
      visible: false
    };
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

  confirm() {
    this.props.onConfirm();
    this.hide();
  }

  render() {
    return (
      <Modal show={this.state.visible} onHide={() => this.hide()}>
        <Modal.Body>
          {this.props.children}
        </Modal.Body>
        <Modal.Footer>
          <Button bsStyle="default" onClick={() => this.hide()}>Cancel</Button>
          <Button bsStyle={this.props.buttonStyle} onClick={() => this.confirm()}>{this.props.action}</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}
