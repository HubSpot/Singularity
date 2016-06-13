import React from 'react';
import Modal from 'react-bootstrap/lib/Modal';
import JSONTree from 'react-json-tree'

export default class JSONButton extends React.Component {

  constructor() {
    super();
    this.state = {
      modalOpen: false
    }
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
    const theme = {
      scheme: 'twilight',
      author: 'david hart (http://hart-dev.com)',
      base00: '#1e1e1e',
      base01: '#323537',
      base02: '#464b50',
      base03: '#5f5a60',
      base04: '#838184',
      base05: '#a7a7a7',
      base06: '#c3c3c3',
      base07: '#ffffff',
      base08: '#cf6a4c',
      base09: '#cda869',
      base0A: '#f9ee98',
      base0B: '#8f9d6a',
      base0C: '#afc4db',
      base0D: '#7587a6',
      base0E: '#9b859d',
      base0F: '#9b703f'
    };
    return (
      <div>
        <a className="btn btn-default" onClick={this.showJSON.bind(this)}>JSON</a>
        <Modal show={this.state.modalOpen} onHide={this.hideJSON.bind(this)}>
          <Modal.Header closeButton>
            <Modal.Title>Viewing JSON</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <JSONTree
              data={this.props.object}
              shouldExpandNode={() => {return true;}}
              theme={theme}
            />
          </Modal.Body>
        </Modal>
      </div>
    );
  }
}

JSONButton.propTypes = {
  object: React.PropTypes.object.isRequired
};
