import React from 'react';

import { Modal, Button } from 'react-bootstrap';

import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default class ShellCommandLauncher extends React.Component {

  constructor() {
    super();
    this.state = {
      commandAcked: true,
      commandStarted: false,
      commandFileExists: false,
      outputFilename: null
    }
  }

  componentDidMount() {
    this.interval = setInterval(() => {
      this.props.updateTask();
    }, 1000);
    console.log(this.props);
  }

  componentDidUpdate(prevProps, prevState) {
    let timestamp = this.props.shellCommandResponse.timestamp;
    let cmdStatus = _.find(this.props.commandHistory, (c) => c.shellRequest.timestamp == timestamp);
    let latestUpdate = cmdStatus
    console.log(cmdStatus);
  }

  componentWillUnmount() {
    clearInterval(this.interval);
  }

  renderStatusList() {
    return (
      <ul className="status-list">
        <li className={this.state.commandAcked ? 'complete text-success' : 'waiting'}>
          {this.state.commandAcked ? <div className="page-loader loader-small" /> : <Glyphicon iconClass='ok' />}
          Command acknowledged...
        </li>
        <li className={this.state.commandStarted ? 'complete text-success' : 'waiting'}>
          {this.state.commandStarted ? <div className="page-loader loader-small" /> : <Glyphicon iconClass='ok' />}
          Command started...
        </li>
        <li className={this.state.commandFileExists ? 'complete text-success' : 'waiting'}>
          {this.state.commandFileExists ? <div className="page-loader loader-small" /> : <Glyphicon iconClass='ok' />}
          Output file{this.state.outputFilename ? <code> {this.state.outputFilename}</code> : ''} exists...
        </li>
      </ul>
    );
  }

  render() {
    return (
      <Modal show={true} onHide={this.props.close} bsSize="small" backdrop="static">
        <Modal.Header closeButton>
            <Modal.Title>Command queued</Modal.Title>
          </Modal.Header>
        <Modal.Body>
          <div className='constrained-modal'>
            {this.renderStatusList()}
          </div>
        </Modal.Body>
      </Modal>
    );
  }
}
