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
        <a className="btn btn-default" onClick={this.showJSON.bind(this)}>JSON</a>
        <Modal show={this.state.modalOpen} onHide={this.hideJSON.bind(this)}>
          <Modal.Body>
            <JSONTree
              data={this.props.object}
              shouldExpandNode={() => {return true;}}
              theme={JSONTreeTheme}
            />
          </Modal.Body>
          <Modal.Footer>
            <Button bsStyle="info" className="copy-btn" data-clipboard-text={JSON.stringify(this.props.object, null, 2)}>Copy</Button>
            <Button onClick={this.hideJSON.bind(this)}>Close</Button>
          </Modal.Footer>
        </Modal>
      </div>
    );
  }
}

JSONButton.propTypes = {
  object: React.PropTypes.object.isRequired
};
