import React from 'react';

import { Modal, Button } from 'react-bootstrap';

export default class ConfirmationDialog extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      visible: false
    }
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
      <Modal show={this.state.visible} onHide={this.hide.bind(this)}>
        <Modal.Body>
          {this.props.text}
        </Modal.Body>
        <Modal.Footer>
          <Button bsStyle="default" onClick={this.hide.bind(this)}>Cancel</Button>
          <Button bsStyle={this.props.buttonStyle} onClick={this.confirm.bind(this)}>{this.props.action}</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

ConfirmationDialog.propTypes = {
  text: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.object]).isRequired,
  action: React.PropTypes.string.isRequired,
  onConfirm: React.PropTypes.func.isRequired,
  buttonStyle: React.PropTypes.string
};
