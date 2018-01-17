import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import rootComponent from '../../rootComponent';
import Utils from '../../utils';

import { FetchTaskFiles } from '../../actions/api/sandbox';
import {
  FetchTaskStatistics,
  RunCommandOnTask,
  FetchTaskCleanups
} from '../../actions/api/tasks';

import {
  FetchTaskHistory,
  FetchDeployForRequest
} from '../../actions/api/history';
import { FetchPendingDeploys } from '../../actions/api/deploys';
import { FetchTaskS3Logs } from '../../actions/api/logs';
import { refresh, onLoad } from '../../actions/ui/taskDetail';

import { InfoBox, UsageInfo } from '../common/statelessComponents';
import {Alert, Panel} from 'react-bootstrap';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import CollapsableSection from '../common/CollapsableSection';
import NotFound from '../common/NotFound';

import KillTaskButton from '../common/modalButtons/KillTaskButton';

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
import TaskState from './TaskState';
import TaskStatus from './TaskStatus';

const RECENTLY_MODIFIED_SECONDS = 60;

class TaskDetail extends Component {

  static propTypes = {
    task: PropTypes.shape({
      task: PropTypes.shape({
        taskId: PropTypes.shape({
          id: PropTypes.string.isRequired,
          startedAt: PropTypes.number.isRequired,
          requestId: PropTypes.string.isRequired,
          deployId: PropTypes.string.isRequired,
          instanceNo: PropTypes.number.isRequired
        }).isRequired,
        taskRequest: PropTypes.shape({
          request: PropTypes.shape({
            requestType: PropTypes.string.isRequired
          }).isRequired,
          deploy: PropTypes.shape({
            customExecutorCmd: PropTypes.string
          }).isRequired
        }).isRequired,
        offers: PropTypes.arrayOf(PropTypes.shape({
          hostname: PropTypes.string
        })).isRequired,
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
      status: PropTypes.oneOf([TaskStatus.RUNNING, TaskStatus.STOPPED, TaskStatus.NEVER_RAN]),
      isStillRunning: PropTypes.bool,
      isCleaning: PropTypes.bool,
      loadBalancerUpdates: PropTypes.array
    }),
    resourceUsage: PropTypes.shape({
      cpusSystemTimeSecs: PropTypes.number,
      cpusUserTimeSecs: PropTypes.number,
      cpusLimit: PropTypes.number,
      diskUsedBytes: PropTypes.number,
      diskLimitBytes: PropTypes.number,
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
    resourceUsageNotFound: PropTypes.bool.isRequired,
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
    currentFilePath: PropTypes.string,
    taskId: PropTypes.string.isRequired,
    params: PropTypes.object,
    fetchTaskHistory: PropTypes.func.isRequired,
    fetchTaskCleanups: PropTypes.func.isRequired,
    fetchTaskStatistics: PropTypes.func.isRequired,
    fetchTaskFiles: PropTypes.func.isRequired,
    runCommandOnTask: PropTypes.func.isRequired,
    group: PropTypes.object
  };

  constructor(props) {
    super(props);
    this.state = {
      previousUsage: null
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

        file.isRecentlyModified = Date.now() / 1000 - file.mtime <= RECENTLY_MODIFIED_SECONDS;

        if (!file.isDirectory) {
          const regex = /(?:\.([^.]+))?$/;
          const extension = regex.exec(file.name)[1];
          file.isTailable = !_.contains(['zip', 'gz', 'jar', 'bz2', 'so', 'png', 'jpg', 'jpeg', 'pdf'], extension);
        }
      }
      return files;
    }
    return {};
  }

  renderFiles(files) {
    if (!files || _.isUndefined(files.currentDirectory)) {
      let message;
      if (this.props.task.isStillRunning) {
        message = 'Could not retrieve files. The task may still be starting.';
      } else {
        message = 'Could not retrieve files. The directory may have already been cleaned up.';
      }
      return (
        <Section title="Files">
          <div className="empty-table-message">
            {message}
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
            if (!_.isUndefined(path) && path.startsWith('/')) path = path.substring(1);
            this.props.fetchTaskFiles(this.props.params.taskId, path).then(() => {
              this.props.router.push(Utils.joinPath(`task/${this.props.params.taskId}/files/`, path));
            });
          }}
        />
      </Section>
    );
  }

  renderHeader(cleanup) {
    const cleaningUpdate = _.find(Utils.maybe(this.props.task, ['taskUpdates'], []), (taskUpdate) => {
      return taskUpdate.taskState === 'TASK_CLEANING';
    });

    let cleanupType;
    if (cleaningUpdate) {
      cleanupType = cleaningUpdate.statusMessage.split(/\s+/)[0];
    } else if (cleanup) {
      cleanupType = cleanup.cleanupType;
    }

    const taskState = (
      <TaskState
        status={this.props.task.status}
        updates={this.props.task.taskUpdates}
        cleanupType={cleanupType}
      />
    );

    let destroy = false;
    let removeText = 'Kill Task';
    if (cleanupType) {
      if (Utils.isImmediateCleanup(cleanupType, Utils.request.isLongRunning(this.props.task.task.taskRequest))) {
        removeText = 'Destroy Task';
        destroy = true;
      } else {
        removeText = 'Override cleanup';
      }
    }

    const refreshHistoryAndCleanups = () => {
      const promises = [];
      promises.push(this.props.fetchTaskCleanups());
      promises.push(this.props.fetchTaskHistory(this.props.params.taskId));
      return Promise.all(promises);
    };

    const removeBtn = this.props.task.isStillRunning && (
      <KillTaskButton
        name={removeText}
        taskId={this.props.params.taskId}
        destroy={destroy}
        then={refreshHistoryAndCleanups}
        shouldShowWaitForReplacementTask={Utils.isIn(this.props.task.task.taskRequest.request.requestType, ['SERVICE', 'WORKER']) && !destroy}
      >
        <a className="btn btn-danger">
          {removeText}
        </a>
      </KillTaskButton>
    );
    const terminationAlert = this.props.task.isStillRunning && this.props.task.isCleaning && destroy && (
      <Alert bsStyle="warning">
          <strong>Task is terminating:</strong> To issue a non-graceful termination (kill -term), click Destroy Task.
      </Alert>
    );
    const breadcrumbs = [
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
    ];
    if (this.props.group) {
      breadcrumbs.unshift({
        label: 'Group',
        text: this.props.group.id,
        link: `group/${this.props.group.id}`
      });
    }

    return (
      <header className="detail-header">
        <div className="row">
          <div className="col-md-12">
            <Breadcrumbs
              items={breadcrumbs}
              right={<span><strong>Hostname: </strong>{this.props.task.task.offers[0].hostname}</span>}
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
    return (this.props.task.isStillRunning || this.props.task.isCleaning || this.props.task.shellCommandHistory.length > 0) && (
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

    let maybeResourceUsage;

    if (this.props.resourceUsageNotFound) {
      maybeResourceUsage = (
        <div className="empty-table-message">
          Could not establish communication with the slave to find resource usage.
        </div>
      );
    } else {
      const maybeCpuUsage = this.props.resourceUsage.cpusLimit > 0 ? (
        <UsageInfo
          title="CPU Usage"
          style={cpuUsageExceeding ? 'danger' : 'success'}
          total={this.props.resourceUsage.cpusLimit}
          used={Math.round(cpuUsage * 100) / 100}
        >
          <span>
            <p>
              {Math.round(cpuUsage * 100) / 100} used / {this.props.resourceUsage.cpusLimit} allocated CPUs
            </p>
            {exceedingWarning}
          </span>
        </UsageInfo>
      ) : (
        <Panel header="CPU Usage">
          <p>{Math.round(cpuUsage * 100) / 100} shares used</p>
        </Panel>
      );
      maybeResourceUsage = (
        <div>
          <div className="row">
            <div className="col-md-3 col-sm-4">
              <UsageInfo
                title="Memory (rss vs limit)"
                style="success"
                total={this.props.resourceUsage.memLimitBytes}
                used={this.props.resourceUsage.memRssBytes}
              >
                {Utils.humanizeFileSize(this.props.resourceUsage.memRssBytes)} / {Utils.humanizeFileSize(this.props.resourceUsage.memLimitBytes)}
              </UsageInfo>
            </div>
            <div className="col-md-3 col-sm-4">
              {maybeCpuUsage}
            </div>
            <div className="col-md-3 col-sm-4">
              <UsageInfo
                title="Disk"
                style={this.props.resourceUsage.diskUsedBytes > this.props.resourceUsage.diskLimitBytes ? 'danger' : 'success'}
                total={this.props.resourceUsage.diskLimitBytes}
                used={this.props.resourceUsage.diskUsedBytes}
              >
                {Utils.humanizeFileSize(this.props.resourceUsage.diskUsedBytes)} / {Utils.humanizeFileSize(this.props.resourceUsage.diskLimitBytes)}
              </UsageInfo>
            </div>
          </div>
          <div className="row">
            <div className="col-md-12">
              <ul className="list-unstyled horizontal-description-list">
                {!!this.props.resourceUsage.cpusNrPeriods && <InfoBox name="CPUs number of periods" value={this.props.resourceUsage.cpusNrPeriods} />}
                {!!this.props.resourceUsage.cpusNrThrottled && <InfoBox name="CPUs number throttled" value={this.props.resourceUsage.cpusNrThrottled} />}
                {!!this.props.resourceUsage.cpusThrottledTimeSecs && <InfoBox name="Throttled time (sec)" value={this.props.resourceUsage.cpusThrottledTimeSecs} />}
                <InfoBox name="Memory (anon)" value={Utils.humanizeFileSize(this.props.resourceUsage.memAnonBytes)} />
                <InfoBox name="Memory (file)" value={Utils.humanizeFileSize(this.props.resourceUsage.memFileBytes)} />
                <InfoBox name="Memory (mapped file)" value={Utils.humanizeFileSize(this.props.resourceUsage.memMappedFileBytes)} />
              </ul>
            </div>
          </div>
        </div>
      );
    }

    return (
      <CollapsableSection title="Resource Usage">
        {maybeResourceUsage}
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
    const filesToDisplay = this.props.files[`${this.props.params.taskId}/${this.props.currentFilePath}`] && this.analyzeFiles(this.props.files[`${this.props.taskId}/${this.props.currentFilePath}`].data);
    const topLevelFiles = this.props.files[`${this.props.params.taskId}/`] && this.analyzeFiles(this.props.files[`${this.props.taskId}/`].data);
    const filesAvailable = topLevelFiles && !_.isEmpty(topLevelFiles.files);

    return (
      <div className="task-detail detail-view">
        {this.renderHeader(cleanup)}
        <TaskAlerts task={this.props.task} deploy={this.props.deploy} pendingDeploys={this.props.pendingDeploys} />
        <TaskMetadataAlerts task={this.props.task} />
        <TaskHistory taskUpdates={this.props.task.taskUpdates} />
        <TaskLatestLog taskId={this.props.taskId} status={this.props.task.status} files={filesToDisplay} available={filesAvailable} />
        {this.renderFiles(filesToDisplay)}
        {_.isEmpty(this.props.s3Logs) || <TaskS3Logs taskId={this.props.task.task.taskId.id} s3Files={this.props.s3Logs} taskStartedAt={this.props.task.task.taskId.startedAt} />}
        {_.isEmpty(this.props.task.loadBalancerUpdates) || <TaskLbUpdates loadBalancerUpdates={this.props.task.loadBalancerUpdates} />}
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
  const { healthcheckResults } = task;
  task.hasSuccessfulHealthcheck = healthcheckResults && healthcheckResults.length > 0 && !!_.find(healthcheckResults, (healthcheckResult) => healthcheckResult.statusCode === 200);
  task.lastHealthcheckFailed = healthcheckResults && healthcheckResults.length > 0 && _.last(healthcheckResults).statusCode !== 200;
  if (healthcheckResults && task.task.taskRequest.deploy && task.task.taskRequest.deploy.healthcheck && task.task.taskRequest.deploy.healthcheck.maxRetries && task.task.taskRequest.deploy.healthcheck.maxRetries > 0) {
    task.tooManyRetries = healthcheckResults.length > task.task.taskRequest.deploy.healthcheck.maxRetries;
  } else {
    task.tooManyRetries = false;
  }
  return task;
}

function mapTaskToProps(task) {
  task.lastKnownState = _.last(task.taskUpdates);
  let isStillRunning = true;
  let status = TaskStatus.RUNNING;

  if (task.taskUpdates && _.contains(Utils.TERMINAL_TASK_STATES, task.lastKnownState.taskState)) {
    if (_.contains(_.map(task.taskUpdates, (update) => update.taskState), 'TASK_RUNNING')) {
      status = TaskStatus.STOPPED;
    } else {
      status = TaskStatus.NEVER_RAN;
    }
    isStillRunning = false;
  }
  task.isStillRunning = isStillRunning;
  task.status = status;

  task.isCleaning = task.lastKnownState && task.lastKnownState.taskState === 'TASK_CLEANING';

  const ports = [];
  if (task.task && task.task.taskRequest.deploy && task.task.taskRequest.deploy.resources && task.task.taskRequest.deploy.resources.numPorts > 0) {
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
  const apiCallData = state.api.task[ownProps.params.taskId];
  let task = apiCallData;
  if (!(task && task.data)) return {};
  if (apiCallData.statusCode === 404) {
    return {
      notFound: true,
      pathname: ownProps.location.pathname
    };
  }
  task = mapTaskToProps(task.data);
  task = mapHealthchecksToProps(task);
  const defaultFilePath = _.isUndefined(ownProps.files) ? '' : ownProps.files.currentDirectory;

  return {
    task,
    taskId: ownProps.params.taskId,
    currentFilePath: _.isUndefined(ownProps.params.splat) ? defaultFilePath : ownProps.params.splat.substring(1),
    taskCleanups: state.api.taskCleanups.data,
    files: state.api.taskFiles,
    resourceUsageNotFound: state.api.taskResourceUsage.statusCode === 404,
    resourceUsage: state.api.taskResourceUsage.data,
    cpuTimestamp: state.api.taskResourceUsage.data.timestamp,
    s3Logs: state.api.taskS3Logs.data,
    deploy: state.api.deploy.data,
    pendingDeploys: state.api.deploys.data,
    shellCommandResponse: state.api.taskShellCommandResponse.data,
    group: task.task && _.first(_.filter(state.api.requestGroups.data, (filterGroup) => _.contains(filterGroup.requestIds, task.task.taskId.requestId)))
  };
}

function mapDispatchToProps(dispatch) {
  return {
    runCommandOnTask: (taskId, commandName) => dispatch(RunCommandOnTask.trigger(taskId, commandName)),
    fetchTaskHistory: (taskId) => dispatch(FetchTaskHistory.trigger(taskId, true)),
    fetchTaskStatistics: (taskId) => dispatch(FetchTaskStatistics.trigger(taskId, [404])),
    fetchTaskFiles: (taskId, path, catchStatusCodes = []) => dispatch(FetchTaskFiles.trigger(taskId, path, catchStatusCodes.concat([404]))),
    fetchDeployForRequest: (taskId, deployId) => dispatch(FetchDeployForRequest.trigger(taskId, deployId)),
    fetchTaskCleanups: () => dispatch(FetchTaskCleanups.trigger()),
    fetchPendingDeploys: () => dispatch(FetchPendingDeploys.trigger()),
    fechS3Logs: (taskId) => dispatch(FetchTaskS3Logs.trigger(taskId, [404])),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(withRouter(TaskDetail), (props) => refresh(props.params.taskId, props.params.splat), true, true, null, (props) => onLoad(props.params.taskId)));
