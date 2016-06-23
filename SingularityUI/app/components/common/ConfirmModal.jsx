import React, { Component, PropTypes } from 'react';
import Modal from 'react-bootstrap/lib/Modal';
import Button from 'react-bootstrap/lib/Button';

export default class ConfirmModal extends Component {
  static propTypes = {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.node),
      PropTypes.node
    ]).isRequired,
    button: PropTypes.node.isRequired,
    confirm: PropTypes.func.isRequired,
    'data-action': PropTypes.string,
    alt: PropTypes.string,
    className: PropTypes.string,
    linkClassName: PropTypes.string
  };

  static defaultProps = {
    onShow: () => true
  };

  constructor() {
    super();
    this.state = {
      modalOpen: false
    }
  }

  show() {
    this.setState({
      modalOpen: true
    });
  }

  hide() {
    this.setState({
      modalOpen: false
    });
  }

  confirm() {
    this.props.confirm();
    this.hide();
  }

  render() {
    return (
      <span className={this.props.className}>
        <a className={this.props.linkClassName} onClick={this.show.bind(this)} data-action={this.props['data-action']} alt={this.props.alt}>
          {this.props.button}
        </a>
        <Modal show={this.state.modalOpen} onHide={this.hide.bind(this)} bsSize='large'>
          <Modal.Body>
            <div className='constrained-modal'>
              {this.props.children}
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button bsStyle='default' onClick={this.hide.bind(this)}>Cancel</Button>
            <Button bsStyle='info' onClick={this.confirm.bind(this)}>Ok</Button>
          </Modal.Footer>
        </Modal>
      </span>
    );
  }
}
