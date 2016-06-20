import React from 'react';

import { Modal, Button } from 'react-bootstrap';

import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default class ShellCommandLauncher extends React.Component {

  constructor() {
    super();
    this.state = {
      commandAcked: false,
      commandStarted: false,
      commandFileExists: false,
      outputFilename: null,
      commandFailed: false,
      commandFailedMessage: null
    }
  }

  componentDidMount() {
    this.props.updateTask();
    this.taskInterval = setInterval(() => {
      this.props.updateTask();
    }, 1000);
  }

  componentWillUpdate(nextProps, nextState) {
    if (nextState.commandFailed) {
      clearInterval(this.taskInterval);
    }
    if (nextState.commandAcked && nextState.commandStarted) {
      clearInterval(this.taskInterval);
      this.fileInterval = setInterval(() => {

      }, 1000);
    }
  }

  componentWillReceiveProps(nextProps) {
    let timestamp = nextProps.shellCommandResponse.timestamp;
    let cmdStatus = _.find(nextProps.commandHistory, (c) => c.shellRequest.timestamp == timestamp);
    if (!cmdStatus || !cmdStatus.shellUpdates) return;
    let failedStatus = _.find(cmdStatus.shellUpdates, (u) => u.updateType == 'FAILED' || u.updateType == 'INVALID');
    let ackedStatus = _.find(cmdStatus.shellUpdates, (u) => u.updateType == 'ACKED');
    this.setState({
      commandAcked: !!ackedStatus,
      commandStarted: !!_.find(cmdStatus.shellUpdates, (u) => u.updateType == 'STARTED'),
      commandFailed: !!failedStatus,
      commandFailedMessage: failedStatus ? failedStatus.message : null
    });
    if (ackedStatus) {
      this.outputFilename = ackedStatus.outputFilename;
    }
    console.log(cmdStatus);
  }

  componentWillUnmount() {
    clearInterval(this.taskInterval);
  }

  renderStatusList() {
    return (
      <ul className="status-list">
        <li className={this.state.commandAcked ? 'complete text-success' : 'waiting'}>
          {!this.state.commandAcked ? <div className="page-loader loader-small" /> : <Glyphicon iconClass='ok' />} Command acknowledged...
        </li>
        <li className={this.state.commandStarted ? 'complete text-success' : 'waiting'}>
          {!this.state.commandStarted ? <div className="page-loader loader-small" /> : <Glyphicon iconClass='ok' />} Command started...
        </li>
        <li className={this.state.commandFileExists ? 'complete text-success' : 'waiting'}>
          {!this.state.commandFileExists ? <div className="page-loader loader-small" /> : <Glyphicon iconClass='ok' />} Output file{this.state.outputFilename ? <code> {this.state.outputFilename}</code> : ''} exists...
        </li>
      </ul>
    );
  }

  render() {
    return (
      <Modal show={true} onHide={this.props.close} bsSize="small" backdrop="static">
        <Modal.Header closeButton>
            <Modal.Title>Redirecting to output</Modal.Title>
          </Modal.Header>
        <Modal.Body>
          <div className='constrained-modal'>
            {this.renderStatusList()}
            {this.state.commandFailed ? (
              <p className="text-danger">
                <Glyphicon iconClass='remove' /> Command failed: {this.state.commandFailedMessage}
              </p>
            ): null}
          </div>
        </Modal.Body>
      </Modal>
    );
  }
}
