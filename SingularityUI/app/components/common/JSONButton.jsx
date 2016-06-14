import React from 'react';
import Modal from 'react-bootstrap/lib/Modal';
import Button from 'react-bootstrap/lib/Button';
import JSONTree from 'react-json-tree'
import { JSONTreeTheme } from '../../thirdPartyConfigurations';
import Clipboard from 'clipboard';

export default class JSONButton extends React.Component {

  constructor() {
    super();
    this.state = {
      modalOpen: false
    }
  }

  componentDidMount() {
    new Clipboard('.copy-btn');
  }

  showJSON() {
    this.setState({
      modalOpen: true
    });
  }

  hideJSON() {
    this.setState({
      modalOpen: false
    });
  }

  render() {
    return (
      <div>
        <a className={this.props.linkClassName} onClick={this.showJSON.bind(this)}>{this.props.text}</a>
        <Modal show={this.state.modalOpen} onHide={this.hideJSON.bind(this)} bsSize="large">
          <Modal.Body>
            <div className="constrained-modal">
              <JSONTree
                data={this.props.object}
                shouldExpandNode={() => {return true;}}
                theme={JSONTreeTheme}
              />
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button bsStyle="default" className="copy-btn" data-clipboard-text={JSON.stringify(this.props.object, null, 2)}>Copy</Button>
            <Button bsStyle="info" onClick={this.hideJSON.bind(this)}>Close</Button>
          </Modal.Footer>
        </Modal>
      </div>
    );
  }
}

JSONButton.propTypes = {
  object: React.PropTypes.object.isRequired
};
