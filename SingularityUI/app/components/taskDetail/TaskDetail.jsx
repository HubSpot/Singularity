import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import rootComponent from '../../rootComponent';
import Utils from '../../utils';

import { FetchTaskFiles } from '../../actions/api/sandbox';
import {
  FetchTaskStatistics,
  KillTask,
  RunCommandOnTask,
  FetchTaskCleanups
} from '../../actions/api/tasks';

import {
  FetchTaskHistory,
  FetchDeployForRequest
} from '../../actions/api/history';
import { FetchPendingDeploys } from '../../actions/api/deploys';
import { FetchTaskS3Logs } from '../../actions/api/logs';

import { InfoBox, UsageInfo } from '../common/statelessComponents';
import { Alert } from 'react-bootstrap';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import FormModal from '../common/modal/FormModal';
import CollapsableSection from '../common/CollapsableSection';
import NotFound from '../common/NotFound';

import TaskFileBrowser from './TaskFileBrowser';
import ShellCommands from './ShellCommands';
import TaskAlerts from './TaskAlerts';
import TaskMetadataAlerts from './TaskMetadataAlerts';
import TaskHistory from './TaskHistory';
import TaskLatestLog from './TaskLatestLog';
import TaskS3Logs from './TaskS3Logs';
import TaskLbUpdates from './TaskLbUpdates';
import TaskInfo from './TaskInfo';
import TaskEnvVars from './TaskEnvVars';
import TaskHealthchecks from './TaskHealthchecks';

class TaskDetail extends Component {

  static propTypes = {
    task: PropTypes.shape({
      task: PropTypes.shape({
        taskId: PropTypes.shape({
          id: PropTypes.string.isRequired,
          requestId: PropTypes.string.isRequired,
          deployId: PropTypes.string.isRequired,
          instanceNo: PropTypes.number.isRequired
        }).isRequired,
        taskRequest: PropTypes.shape({
          deploy: PropTypes.shape({
            customExecutorCmd: PropTypes.string
          }).isRequired
        }).isRequired,
        offer: PropTypes.shape({
          hostname: PropTypes.string
        }).isRequired,
        mesosTask: PropTypes.shape({
          executor: PropTypes.object
        }).isRequired,
      }).isRequired,
      shellCommandHistory: PropTypes.array.isRequired,
      taskUpdates: PropTypes.arrayOf(PropTypes.shape({
        taskState: PropTypes.string
      })),
      healthcheckResults: PropTypes.array,
      ports: PropTypes.array,
      directory: PropTypes.string,
      isStillRunning: PropTypes.bool,
      isCleaning: PropTypes.bool
    }),
    resourceUsage: PropTypes.shape({
      cpusSystemTimeSecs: PropTypes.number,
      cpusUserTimeSecs: PropTypes.number,
      cpusLimit: PropTypes.number,
      memLimitBytes: PropTypes.number,
      memRssBytes: PropTypes.number,
      cpusNrPeriods: PropTypes.number,
      cpusNrThrottled: PropTypes.number,
      cpusThrottledTimeSecs: PropTypes.number,
      memAnonBytes: PropTypes.number,
      memFileBytes: PropTypes.number,
      memMappedFileBytes: PropTypes.number,
      timestamp: PropTypes.number
    }),
    taskCleanups: PropTypes.arrayOf(PropTypes.shape({
      taskId: PropTypes.shape({
        id: PropTypes.string
      }).isRequired
    })).isRequired,
    router: PropTypes.object.isRequired,
    s3Logs: PropTypes.array,
    deploy: PropTypes.object,
    pendingDeploys: PropTypes.array,
    shellCommandResponse: PropTypes.object,
    files: PropTypes.shape({
      files: PropTypes.array,
      currentDirectory: PropTypes.string
    }),
    filePath: PropTypes.string,
    taskId: PropTypes.string.isRequired,
    params: PropTypes.object,
    fetchTaskHistory: PropTypes.func.isRequired,
    fetchTaskStatistics: PropTypes.func.isRequired,
    fetchTaskFiles: PropTypes.func.isRequired,
    killTask: PropTypes.func.isRequired,
    runCommandOnTask: PropTypes.func.isRequired
  };

  constructor(props) {
    super(props);
    this.state = {
      previousUsage: null,
      currentFilePath: props.params.splat || props.params.taskId
    };
  }

  componentDidMount() {
    if (!this.props.task) return;
    // Get a second sample for CPU usage right away
    if (this.props.task.isStillRunning) {
      this.props.fetchTaskStatistics(this.props.params.taskId);
    }
  }

  componentWillReceiveProps(nextProps) {
    if (!this.props.task) return;
    if (nextProps.resourceUsage.timestamp !== this.props.resourceUsage.timestamp) {
      this.setState({
        previousUsage: this.props.resourceUsage
      });
    }
  }

  analyzeFiles(files) {
    if (files && files.files) {
      for (const file of files.files) {
        file.isDirectory = file.mode[0] === 'd';
        let httpPrefix = 'http';
        let httpPort = config.slaveHttpPort;
        if (config.slaveHttpsPort) {
          httpPrefix = 'https';
          httpPort = config.slaveHttpsPort;
        }

        if (files.currentDirectory) {
          file.uiPath = `${files.currentDirectory}/${file.name}`;
        } else {
          file.uiPath = file.name;
        }

        file.fullPath = `${files.fullPathToRoot}/${files.currentDirectory}/${file.name}`;
        file.downloadLink = `${httpPrefix}://${files.slaveHostname}:${httpPort}/files/download.json?path=${file.fullPath}`;

        if (!file.isDirectory) {
          const regex = /(?:\.([^.]+))?$/;
          const extension = regex.exec(file.name)[1];
          file.isTailable = !_.contains(['zip', 'gz', 'jar'], extension);
        }
      }
    }
    return files;
  }

  killTask(data) {
    this.props.killTask(this.props.params.taskId, data).then(() => {
      this.props.fetchTaskHistory(this.props.params.taskId);
    });
  }

  renderFiles(files) {
    if (!files || _.isUndefined(files.currentDirectory)) {
      return (
        <Section title="Files">
          <div className="empty-table-message">
            {'Could not retrieve files. The host of this task is likely offline or its directory has been cleaned up.'}
          </div>
        </Section>
      );
    }
    return (
      <Section title="Files">
        <TaskFileBrowser
          taskId={this.props.taskId}
          files={files.files}
          currentDirectory={files.currentDirectory}
          changeDir={(path) => {
            if (path.startsWith('/')) path = path.substring(1);
            this.props.fetchTaskFiles(this.props.params.taskId, path).then(() => {
              this.setState({
                currentFilePath: path
              });
              this.props.router.push(Utils.joinPath(`task/${this.props.params.taskId}/files/`, path));
            });
          }}
        />
      </Section>
    );
  }

  renderHeader(cleanup) {
    const taskState = this.props.task.taskUpdates && (
      <div className="col-xs-6 task-state-header">
        <h1>
          <span className={`label label-${Utils.getLabelClassFromTaskState(_.last(this.props.task.taskUpdates).taskState)} task-state-header-label`}>
            {Utils.humanizeText(_.last(this.props.task.taskUpdates).taskState)} {cleanup && `(${Utils.humanizeText(cleanup.cleanupType)})`}
          </span>
        </h1>
      </div>
    );

    let removeText;
    if (cleanup) {
      removeText = cleanup.isImmediate ? 'Destroy task' : 'Override cleanup';
    } else {
      removeText = this.props.task.isCleaning ? 'Destroy task' : 'Kill Task';
    }
    const removeBtn = this.props.task.isStillRunning && (
      <span>
        <FormModal
          ref="confirmKillTask"
          action={removeText}
          onConfirm={(event) => this.killTask(event)}
          buttonStyle="danger"
          formElements={[
            {
              name: 'waitForReplacementTask',
              type: FormModal.INPUT_TYPES.BOOLEAN,
              label: 'Wait for replacement task to start before killing task',
              defaultValue: true
            },
            {
              name: 'message',
              type: FormModal.INPUT_TYPES.STRING,
              label: 'Message (optional)'
            }
          ]}>
          <span>
            <p>Are you sure you want to kill this task?</p>
            <pre>{this.props.params.taskId}</pre>
            <p>
                Long running process will be started again instantly, scheduled
                tasks will behave as if the task failed and may be rescheduled
                to run in the future depending on whether or not the request
                has <code>numRetriesOnFailure</code> set.
            </p>
          </span>
        </FormModal>
        <a className="btn btn-danger" onClick={() => this.refs.confirmKillTask.show()}>
          {removeText}
        </a>
      </span>
    );
    const terminationAlert = this.props.task.isStillRunning && !cleanup && this.props.task.isCleaning && (
      <Alert bsStyle="warning">
          <strong>Task is terminating:</strong> To issue a non-graceful termination (kill -term), click Destroy Task.
      </Alert>
    );

    return (
      <header className="detail-header">
        <div className="row">
          <div className="col-md-12">
            <Breadcrumbs
              items={[
                {
                  label: 'Request',
                  text: this.props.task.task.taskId.requestId,
                  link: `request/${this.props.task.task.taskId.requestId}`
                },
                {
                  label: 'Deploy',
                  text: this.props.task.task.taskId.deployId,
                  link: `request/${this.props.task.task.taskId.requestId}/deploy/${this.props.task.task.taskId.deployId}`
                },
                {
                  label: 'Instance',
                  text: this.props.task.task.taskId.instanceNo,
                }
              ]}
              right={<span><strong>Hostname: </strong>{this.props.task.task.offer.hostname}</span>}
            />
          </div>
        </div>
        <div className="row">
          {taskState}
          <div className={`col-xs-${taskState ? '6' : '12'} button-container`}>
            <JSONButton object={this.props.task} linkClassName="btn btn-default">
              JSON
            </JSONButton>
            {removeBtn}
          </div>
        </div>
        {terminationAlert}
      </header>
    );
  }

  renderShellCommands() {
    return (this.props.task.isStillRunning || this.props.task.shellCommandHistory.length > 0) && (
      <CollapsableSection title="Shell commands">
        <ShellCommands
          customExecutorCmd={this.props.task.task.taskRequest.deploy.customExecutorCmd}
          isStillRunning={this.props.task.isStillRunning}
          shellCommandHistory={this.props.task.shellCommandHistory}
          taskFiles={this.props.files}
          shellCommandResponse={this.props.shellCommandResponse}
          runShellCommand={(commandName) => {
            return this.props.runCommandOnTask(this.props.taskId, commandName);
          }}
          updateTask={() => {
            this.props.fetchTaskHistory(this.props.taskId);
          }}
          updateFiles={(path) => {
            this.props.fetchTaskFiles(this.props.taskId, path);
          }}
        />
      </CollapsableSection>
    );
  }

  renderResourceUsage() {
    if (!this.props.task.isStillRunning) return null;
    let cpuUsage = 0;
    let cpuUsageExceeding = false;
    if (this.state.previousUsage) {
      const currentTime = this.props.resourceUsage.cpusSystemTimeSecs + this.props.resourceUsage.cpusUserTimeSecs;
      const previousTime = this.state.previousUsage.cpusSystemTimeSecs + this.state.previousUsage.cpusUserTimeSecs;
      const timestampDiff = this.props.resourceUsage.timestamp - this.state.previousUsage.timestamp;
      cpuUsage = (currentTime - previousTime) / timestampDiff;
      cpuUsageExceeding = (cpuUsage / this.props.resourceUsage.cpusLimit) > 1.10;
    }

    const exceedingWarning = cpuUsageExceeding && (
      <span className="label label-danger">CPU usage > 110% allocated</span>
    );

    return (
      <CollapsableSection title="Resource Usage">
        <div className="row">
          <div className="col-md-3">
            <UsageInfo
              title="Memory (rss vs limit)"
              style="success"
              total={this.props.resourceUsage.memLimitBytes}
              used={this.props.resourceUsage.memRssBytes}
              text={`${Utils.humanizeFileSize(this.props.resourceUsage.memRssBytes)} / ${Utils.humanizeFileSize(this.props.resourceUsage.memLimitBytes)}`}
            />
            <UsageInfo
              title="CPU Usage"
              style={cpuUsageExceeding ? 'danger' : 'success'}
              total={this.props.resourceUsage.cpusLimit}
              used={Math.round(cpuUsage * 100) / 100}
              text={<span><p>{`${Math.round(cpuUsage * 100) / 100} used / ${this.props.resourceUsage.cpusLimit} allocated CPUs`}</p>{exceedingWarning}</span>}
            />
          </div>
          <div className="col-md-9">
            <ul className="list-unstyled horizontal-description-list">
              {this.props.resourceUsage.cpusNrPeriods && <InfoBox copyableClassName="info-copyable" name="CPUs number of periods" value={this.props.resourceUsage.cpusNrPeriods} />}
              {this.props.resourceUsage.cpusNrThrottled && <InfoBox copyableClassName="info-copyable" name="CPUs number throttled" value={this.props.resourceUsage.cpusNrThrottled} />}
              {this.props.resourceUsage.cpusThrottledTimeSecs && <InfoBox copyableClassName="info-copyable" name="Throttled time (sec)" value={this.props.resourceUsage.cpusThrottledTimeSecs} />}
              <InfoBox copyableClassName="info-copyable" name="Memory (anon)" value={Utils.humanizeFileSize(this.props.resourceUsage.memAnonBytes)} />
              <InfoBox copyableClassName="info-copyable" name="Memory (file)" value={Utils.humanizeFileSize(this.props.resourceUsage.memFileBytes)} />
              <InfoBox copyableClassName="info-copyable" name="Memory (mapped file)" value={Utils.humanizeFileSize(this.props.resourceUsage.memMappedFileBytes)} />
            </ul>
          </div>
        </div>
      </CollapsableSection>
    );
  }

  render() {
    if (!this.props.task) {
      return <NotFound path={`task/ ${this.props.taskId}`} />;
    }
    const cleanup = _.find(this.props.taskCleanups, (cleanupToTest) => {
      return cleanupToTest.taskId.id === this.props.taskId;
    });
    const filesToDisplay = this.props.files[`${this.props.params.taskId}/${this.state.currentFilePath}`] && this.analyzeFiles(this.props.files[`${this.props.taskId}/${this.state.currentFilePath}`].data);

    return (
      <div className="task-detail detail-view">
        {this.renderHeader(cleanup)}
        <TaskAlerts task={this.props.task} deploy={this.props.deploy} pendingDeploys={this.props.pendingDeploys} />
        <TaskMetadataAlerts task={this.props.task} />
        <TaskHistory taskUpdates={this.props.task.taskUpdates} />
        <TaskLatestLog taskId={this.props.taskId} isStillRunning={this.props.task.isStillRunning} />
        {this.renderFiles(filesToDisplay)}
        <TaskS3Logs taskId={this.props.task.task.taskId.id} s3Files={this.props.s3Logs} />
        <TaskLbUpdates task={this.props.task} />
        <TaskInfo task={this.props.task.task} ports={this.props.task.ports} directory={this.props.task.directory} />
        {this.renderResourceUsage()}
        <TaskEnvVars executor={this.props.task.task.mesosTask.executor} />
        <TaskHealthchecks task={this.props.task.task} healthcheckResults={this.props.task.healthcheckResults} ports={this.props.task.ports} />
        {this.renderShellCommands()}
      </div>
    );
  }
}

function mapHealthchecksToProps(task) {
  if (!task) return task;
  const hcs = task.healthcheckResults;
  task.hasSuccessfulHealthcheck = hcs && hcs.length > 0 && !!_.find(hcs, (h) => h.statusCode === 200);
  task.lastHealthcheckFailed = hcs && hcs.length > 0 && hcs[0].statusCode !== 200;
  task.healthcheckFailureReasonMessage = Utils.healthcheckFailureReasonMessage(task);
  task.tooManyRetries = hcs && hcs.length > task.task.taskRequest.deploy.healthcheckMaxRetries && task.task.taskRequest.deploy.healthcheckMaxRetries > 0;
  task.secondsElapsed = task.task && task.task.taskRequest && task.task.taskRequest.deploy.healthcheckMaxTotalTimeoutSeconds || config.defaultDeployHealthTimeoutSeconds;
  return task;
}

function mapTaskToProps(task) {
  task.lastKnownState = _.last(task.taskUpdates);
  let isStillRunning = true;
  if (task.taskUpdates && _.contains(Utils.TERMINAL_TASK_STATES, task.lastKnownState.taskState)) {
    isStillRunning = false;
  }
  task.isStillRunning = isStillRunning;

  task.isCleaning = task.lastKnownState && task.lastKnownState.taskState === 'TASK_CLEANING';

  const ports = [];
  if (task.task && task.task.taskRequest.deploy.resources.numPorts > 0) {
    for (const resource of task.task.mesosTask.resources) {
      if (resource.name === 'ports') {
        for (const range of resource.ranges.range) {
          for (const port of Utils.range(range.begin, range.end + 1)) {
            ports.push(port);
          }
        }
      }
    }
  }
  task.ports = ports;

  return task;
}

function mapStateToProps(state, ownProps) {
  let task = state.api.task[ownProps.params.taskId];
  if (!(task && task.data)) return {};
  task = mapTaskToProps(task.data);
  task = mapHealthchecksToProps(task);
  return {
    task,
    taskId: ownProps.params.taskId,
    taskCleanups: state.api.taskCleanups.data,
    files: state.api.taskFiles,
    resourceUsage: state.api.taskResourceUsage.data,
    cpuTimestamp: state.api.taskResourceUsage.data.timestamp,
    s3Logs: state.api.taskS3Logs.data,
    deploy: state.api.deploy.data,
    pendingDeploys: state.api.deploys.data,
    shellCommandResponse: state.api.taskShellCommandResponse.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    runCommandOnTask: (taskId, commandName) => dispatch(RunCommandOnTask.trigger(taskId, commandName)),
    killTask: (taskId, data) => dispatch(KillTask.trigger(taskId, data)),
    fetchTaskHistory: (taskId) => dispatch(FetchTaskHistory.trigger(taskId)),
    fetchTaskStatistics: (taskId) => dispatch(FetchTaskStatistics.trigger(taskId)),
    fetchTaskFiles: (...args) => dispatch(FetchTaskFiles.trigger(...args)),
    fetchDeployForRequest: (taskId, deployId) => dispatch(FetchDeployForRequest.trigger(taskId, deployId)),
    fetchTaskCleanups: () => dispatch(FetchTaskCleanups.trigger()),
    fetchPendingDeploys: () => dispatch(FetchPendingDeploys.trigger()),
    fechS3Logs: (taskId) => dispatch(FetchTaskS3Logs.trigger(taskId)),
  };
}

function refresh(props) {
  props.fetchTaskFiles(props.params.taskId, props.params.splat || props.params.taskId, [400]);
  const promises = [];
  const taskPromise = props.fetchTaskHistory(props.params.taskId);
  taskPromise.then(() => {
    const task = props.route.store.getState().api.task[props.params.taskId].data;
    promises.push(props.fetchDeployForRequest(task.task.taskId.requestId, task.task.taskId.deployId));
    if (task.isStillRunning) {
      promises.push(props.fetchTaskStatistics(props.params.taskId));
    }
  });
  promises.push(taskPromise);
  promises.push(props.fetchTaskCleanups());
  promises.push(props.fetchPendingDeploys());
  promises.push(props.fechS3Logs(props.params.taskId));
  return Promise.all(promises);
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(withRouter(TaskDetail), (props) => props.params.taskId, refresh));
