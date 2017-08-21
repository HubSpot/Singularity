import React, { Component, PropTypes } from 'react';
import { Modal, Button, MenuItem } from 'react-bootstrap';
import Clipboard from 'clipboard';
import Utils from '../../utils';

import { InfoBox } from '../common/statelessComponents';

export default class MetadataButton extends Component {
  static propTypes = {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.node),
      PropTypes.node
    ]).isRequired,
    metadata: PropTypes.object.isRequired,
    className: PropTypes.string,
    title: PropTypes.string
  };

  constructor() {
    super();
    this.state = {
      modalOpen: false
    };
    _.bindAll(this, 'hide', 'show');
  }

  componentDidMount() {
    this.clipboard = new Clipboard('.copy-btn');
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

  render() {
    const items = [];
    for (const key of _.keys(this.props.metadata)) {
      items.push(
        <InfoBox copyableClassName="info-copyable" key={key} name={Utils.humanizeCamelcase(key)} value={this.props.metadata[key]} />
      );
    }

    return (
      <span className={this.props.className}>
        <MenuItem onClick={this.show} alt="Show Metadata">{this.props.children}</MenuItem>
        <Modal show={this.state.modalOpen} onHide={this.hide} bsSize="large">
          <Modal.Header>
            <Modal.Title>{this.props.title}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div className="constrained-modal">
              <div className="row">
                <ul className="list-unstyled horizontal-description-list">
                  {items}
                </ul>
              </div>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button bsStyle="info" onClick={this.hide}>Close</Button>
          </Modal.Footer>
        </Modal>
      </span>
    );
  }
}
