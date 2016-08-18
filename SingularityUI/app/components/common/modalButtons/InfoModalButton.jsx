import React, { Component, PropTypes } from 'react';
import { Modal, Button } from 'react-bootstrap';
import { InfoButton } from '../statelessComponents';

class InfoModalButton extends Component {
  constructor(props) {
    super(props);
    this.state = { showModal: false };
    _.bindAll(this, 'show', 'hide');
  }

  show() { this.setState({ showModal: true }); }
  hide() { this.setState({ showModal: false }); }

  render() {
    return (
      <span className={this.props.className}>
        <Modal show={this.state.showModal} onHide={this.hide} backdrop={true}>
          {this.props.title && <Modal.Header><h3>{this.props.title}</h3></Modal.Header>}
          <Modal.Body>
            {this.props.children}
          </Modal.Body>
          <Modal.Footer>
          <Button bsStyle="default" onClick={this.hide}>OK</Button>
        </Modal.Footer>
        </Modal>
        <InfoButton onClick={this.show} id={this.props.id} />
      </span>
    );
  }
}

InfoModalButton.propTypes = {
  id: PropTypes.string.isRequired,
  title: PropTypes.string,
  className: PropTypes.string,
  children: PropTypes.node
};

export default InfoModalButton;
