import React from 'react';
import classNames from 'classnames';

import { Modal, Button } from 'react-bootstrap';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default class ShellCommandLauncher extends React.Component {

  constructor() {
    super();
    this.state = {
      visible: false,
      taskStarted: false
    }
  }

  componentWillUnmount() {
    this.clearIntervals();
  }

  startPolling(requestId, runId, tailFilename=null) {
    this.show();
    console.log(requestId, runId, tailFilename);

    // Wait for task to start
    this.taskInterval = setInterval(() => {
      const promises = [];
      promises.push(this.props.fetchTaskRun(requestId, runId));
      promises.push(this.props.fetchTaskRunHistory(requestId, runId));
      Promise.all(promises).then((responses) => {
        responses = _.without(_.pluck(responses, 'data'), undefined);
        console.log(responses);
        if (responses.length) {
          this.clearIntervals();
          this.setState({
            taskStarted: true
          });
          if (tailFilename) {
            this.logFilePoll(tailFilename);
          } else {
            app.router.navigate(`task/${_.first(responses).id}`, {trigger: true});
          }
        }
      });
    }, 1000);
  }

  logFilePoll(filename) {
    this.fileInterval = setInterval(() => {

    }, 1000);
  }

  show() {
    this.setState({
      visible: true
    });
  }

  hide() {
    this.setState({
      visible: false
    });
    this.clearIntervals();
  }

  clearIntervals() {
    clearInterval(this.taskInterval);
    clearInterval(this.fileInterval);
  }

  stepStatus(state, text) {
    return (
      <li className={classNames({'complete text-success': state}, {'waiting': !state})}>
        {!state ? <div className="page-loader loader-small" /> : <Glyphicon iconClass='ok' />} {text}...
      </li>
    );
  }

  renderStatusList() {
    return (
      <ul className="status-list">
        {this.stepStatus(this.state.taskStarted, 'Waiting for task to launch')}
      </ul>
    );
  }

  render() {
    return (
      <Modal show={this.state.visible} onHide={this.hide.bind(this)} bsSize="small" backdrop="static">
        <Modal.Header closeButton>
            <Modal.Title>Launching</Modal.Title>
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
