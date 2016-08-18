import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';

import { Modal, Glyphicon } from 'react-bootstrap';

import { FetchRequestRun } from '../../actions/api/requests';
import { FetchRequestRunHistory } from '../../actions/api/history';
import { FetchTaskFiles } from '../../actions/api/sandbox';

class TaskLauncher extends Component {
  static propTypes = {
    fetchRequestRun: PropTypes.func.isRequired,
    fetchRequestRunHistory: PropTypes.func.isRequired,
    fetchTaskFiles: PropTypes.func.isRequired,
    router: PropTypes.object.isRequired
  };

  constructor() {
    super();
    this.state = {
      visible: false,
      taskStarted: false,
      fileExists: false,
      tailFilename: null
    };
  }

  componentWillUnmount() {
    this.clearIntervals();
  }

  startPolling(requestId, runId, tailFilename = null) {
    this.setState({
      tailFilename
    });
    this.show();

    // Wait for task to start
    this.taskInterval = setInterval(() => {
      const promises = [];
      promises.push(this.props.fetchRequestRun(requestId, runId, [404]));
      promises.push(this.props.fetchRequestRunHistory(requestId, runId, [404]));
      Promise.all(promises).then((responses) => {
        const responseList = _.filter(_.pluck(responses, 'data'), (response) => !!response);
        if (responseList.length) {
          this.clearIntervals();
          this.setState({
            taskStarted: true
          });
          const task = _.first(responseList);
          const taskId = task.taskId ? task.taskId.id : task.id;

          if (tailFilename) {
            this.logFilePoll(taskId, tailFilename);
          } else {
            this.props.router.push(`task/${taskId}`);
          }
        }
      });
    }, 1000);
  }

  logFilePoll(taskId, filename) {
    this.fileInterval = setInterval(() => {
      const directory = filename.indexOf('/') !== -1 ? `/${_.initial(filename.split('/')).join('/')}` : '';
      this.props.fetchTaskFiles(taskId, `${taskId}${directory}`, [400]).then((response) => {
        const files = response.data && response.data.files;
        if (files) {
          const file = _.find(files, (fileToCheck) => fileToCheck.name === _.last(filename.split('/')));
          if (file) {
            this.setState({
              fileExists: true
            });
            this.clearIntervals();
            this.props.router.push(`task/${taskId}/tail/${taskId}/${filename}`);
          }
        }
      });
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
        {!state ? <div className="page-loader loader-small" /> : <Glyphicon glyph="ok" />} {text}...
      </li>
    );
  }

  renderStatusList() {
    const fileExists = this.state.tailFilename && this.stepStatus(this.state.fileExists, `Waiting for ${this.state.tailFilename} to exist`);
    return (
      <ul className="status-list">
        {this.stepStatus(this.state.taskStarted, 'Waiting for task to launch')}
        {fileExists}
      </ul>
    );
  }

  render() {
    return (
      <Modal show={this.state.visible} onHide={() => this.hide()} bsSize="small" backdrop="static">
        <Modal.Header closeButton={true}>
            <Modal.Title>Launching</Modal.Title>
          </Modal.Header>
        <Modal.Body>
          <div className="constrained-modal">
            {this.renderStatusList()}
          </div>
        </Modal.Body>
      </Modal>
    );
  }
}

const mapDispatchToProps = (dispatch) => ({
  fetchRequestRun: (...args) => dispatch(FetchRequestRun.trigger(...args)),
  fetchRequestRunHistory: (...args) => dispatch(FetchRequestRunHistory.trigger(...args)),
  fetchTaskFiles: (...args) => dispatch(FetchTaskFiles.trigger(...args))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(TaskLauncher);
