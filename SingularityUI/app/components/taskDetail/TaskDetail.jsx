import React from 'react';
import { connect } from 'react-redux';
import Utils from '../../utils';

import { FetchTaskFiles } from '../../actions/api/sandbox';
import {
  FetchTaskStatistics,
  KillTask,
  RunCommandOnTask
} from '../../actions/api/tasks';

import {
  FetchTaskHistory,
} from '../../actions/api/history';

import { InfoBox, UsageInfo } from '../common/statelessComponents';
import { Alert } from 'react-bootstrap';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import FormModal from '../common/FormModal';
import CollapsableSection from '../common/CollapsableSection';

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

class TaskDetail extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      previousUsage: null,
      currentFilePath: props.filePath
    };
  }

  componentDidMount() {
    // Get a second sample for CPU usage right away
    if (this.props.task.isStillRunning) {
      this.props.dispatch(FetchTaskStatistics.trigger(this.props.taskId));
    }
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.resourceUsage.timestamp != this.props.resourceUsage.timestamp) {
      this.setState({
        previousUsage: this.props.resourceUsage
      });
    }
  }

  renderHeader(t, cleanup) {
    const taskState = t.taskUpdates && (
      <div className="col-xs-6 task-state-header">
        <h1>
          <span className={`label label-${Utils.getLabelClassFromTaskState(_.last(t.taskUpdates).taskState)} task-state-header-label`}>
            {Utils.humanizeText(_.last(t.taskUpdates).taskState)} {cleanup ? `(${Utils.humanizeText(cleanup.cleanupType)})` : ''}
          </span>
        </h1>
      </div>
    );

    const removeText = cleanup ?
      (cleanup.isImmediate ? 'Destroy task' : 'Override cleanup') :
      (t.isCleaning ? 'Destroy task' : 'Kill Task');
    const removeBtn = t.isStillRunning && (
      <span>
        <FormModal
          ref="confirmKillTask"
          action={removeText}
          onConfirm={this.killTask.bind(this)}
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
            <pre>{this.props.taskId}</pre>
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
    const terminationAlert = t.isStillRunning && !cleanup && t.isCleaning && (
      <Alert bsStyle="warning">
          <strong>Task is terminating:</strong> To issue a non-graceful termination (kill -term), click Destroy Task.
      </Alert>
    );

    return (
      <header className='detail-header'>
        <div className="row">
          <div className="col-md-12">
            <Breadcrumbs
              items={[
                {
                  label: "Request",
                  text: t.task.taskId.requestId,
                  link: `${config.appRoot}/request/${t.task.taskId.requestId}`
                },
                {
                  label: "Deploy",
                  text: t.task.taskId.deployId,
                  link: `${config.appRoot}/request/${t.task.taskId.requestId}/deploy/${t.task.taskId.deployId}`
                },
                {
                  label: "Instance",
                  text: t.task.taskId.instanceNo,
                }
              ]}
              right={<span><strong>Hostname: </strong>{t.task.offer.hostname}</span>}
            />
          </div>
        </div>
        <div className="row">
          {taskState}
          <div className={`col-xs-${taskState ? '6' : '12'} button-container`}>
            <JSONButton object={t} linkClassName="btn btn-default">
              JSON
            </JSONButton>
            {removeBtn}
          </div>
        </div>
        {terminationAlert}
      </header>
    );
  }

  renderFiles(t, files) {
    if (!files || !files.currentDirectory) {
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
          taskId={t.task.taskId.id}
          files={files}
          changeDir={(path) => {
            if (path.startsWith('/')) path = path.substring(1);
            this.props.dispatch(FetchTaskFiles.trigger(this.props.taskId, path)).then(() => {
              this.setState({
                currentFilePath: path
              });
              app.router.navigate(Utils.joinPath(`#task/${this.props.taskId}/files/`, path));
            });
          }}
        />
      </Section>
    );
  }

  renderResourceUsage(t, usage) {
    if (!t.isStillRunning) return null;
    let cpuUsage = 0;
    let cpuUsageExceeding = false;
    if (this.state.previousUsage) {
      let currentTime = usage.cpusSystemTimeSecs + usage.cpusUserTimeSecs;
      let previousTime = this.state.previousUsage.cpusSystemTimeSecs + this.state.previousUsage.cpusUserTimeSecs;
      let timestampDiff = usage.timestamp - this.state.previousUsage.timestamp;
      cpuUsage = (currentTime - previousTime) / timestampDiff;
      cpuUsageExceeding = (cpuUsage / usage.cpusLimit) > 1.10;
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
              total={usage.memLimitBytes}
              used={usage.memRssBytes}
              text={`${Utils.humanizeFileSize(usage.memRssBytes)} / ${Utils.humanizeFileSize(usage.memLimitBytes)}`}
            />
            <UsageInfo
              title="CPU Usage"
              style={cpuUsageExceeding ? "danger" : "success"}
              total={usage.cpusLimit}
              used={Math.round(cpuUsage * 100) / 100}
              text={<span><p>{`${Math.round(cpuUsage * 100) / 100} used / ${usage.cpusLimit} allocated CPUs`}</p>{exceedingWarning}</span>}
            />
          </div>
          <div className='col-md-9'>
            <ul className="list-unstyled horizontal-description-list">
              {usage.cpusNrPeriods ? <InfoBox copyableClassName="info-copyable" name="CPUs number of periods" value={usage.cpusNrPeriods} /> : null}
              {usage.cpusNrThrottled ? <InfoBox copyableClassName="info-copyable" name="CPUs number throttled" value={usage.cpusNrThrottled} />: null}
              {usage.cpusThrottledTimeSecs ? <InfoBox copyableClassName="info-copyable" name="Throttled time (sec)" value={usage.cpusThrottledTimeSecs} />: null}
              <InfoBox copyableClassName="info-copyable" name="Memory (anon)" value={Utils.humanizeFileSize(usage.memAnonBytes)} />
              <InfoBox copyableClassName="info-copyable" name="Memory (file)" value={Utils.humanizeFileSize(usage.memFileBytes)} />
              <InfoBox copyableClassName="info-copyable" name="Memory (mapped file)" value={Utils.humanizeFileSize(usage.memMappedFileBytes)} />
            </ul>
          </div>
        </div>
      </CollapsableSection>
    )
  }

  renderShellCommands(t, shellCommandResponse, taskFiles) {
    if (t.isStillRunning || t.shellCommandHistory.length > 0) {
      return (
        <CollapsableSection title="Shell commands">
          <ShellCommands
            task={t}
            taskFiles={taskFiles}
            shellCommandResponse={shellCommandResponse}
            runShellCommand={(commandName) => {
              return this.props.dispatch(RunCommandOnTask.trigger(this.props.taskId, commandName));
            }}
            updateTask={() => {
              this.props.dispatch(FetchTaskHistory.trigger(this.props.taskId));
            }}
            updateFiles={(path) => {
              this.props.dispatch(FetchTaskFiles.trigger(this.props.taskId, path));
            }}
          />
        </CollapsableSection>
      );
    }
  }

  render() {
    let task = this.props.task;
    let cleanup = _.find(this.props.taskCleanups, (c) => {
      return c.taskId.id == this.props.taskId;
    });
    let filesToDisplay = this.analyzeFiles(this.props.files[`${this.props.taskId}/${this.state.currentFilePath}`].data);

    return (
      <div className="task-detail detail-view">
        {this.renderHeader(task, cleanup)}
        <TaskAlerts task={task} deploy={this.props.deploy} pendingDeploys={this.props.pendingDeploys} />
        <TaskMetadataAlerts task={task} />
        <TaskHistory task={task} />
        <TaskLatestLog task={task} />
        {this.renderFiles(task, filesToDisplay)}
        <TaskS3Logs task={task} s3Files={this.props.s3Logs} />
        <TaskLbUpdates task={task} />
        <TaskInfo task={task} />
        {this.renderResourceUsage(task, this.props.resourceUsage)}
        <TaskEnvVars task={task} />
        <TaskHealthchecks task={task} />
        {this.renderShellCommands(task, this.props.shellCommandResponse, this.props.files)}
      </div>
    );
  }

  analyzeFiles(files) {
    if (files && files.files) {
      for (let f of files.files) {
        f.isDirectory = f.mode[0] == 'd';
        let httpPrefix = "http";
        let httpPort = config.slaveHttpPort;
        if (config.slaveHttpsPort) {
          httpPrefix = "https";
          httpPort = config.slaveHttpsPort;
        }

        if (files.currentDirectory) {
          f.uiPath = files.currentDirectory + "/" + f.name;
        } else {
          f.uiPath = f.name;
        }

        f.fullPath = files.fullPathToRoot + '/' + files.currentDirectory + '/' + f.name;
        f.downloadLink = `${httpPrefix}://${files.slaveHostname}:${httpPort}/files/download.json?path=${f.fullPath}`;

        if (!f.isDirectory) {
          let re = /(?:\.([^.]+))?$/;
          let extension = re.exec(f.name)[1];
          f.isTailable = !_.contains(['zip', 'gz', 'jar'], extension);
        }
      }
    }
    return files;
  }

  killTask(data) {
    this.props.dispatch(KillTask.trigger(this.props.taskId, data)).then((e) =>{
      this.props.dispatch(FetchTaskHistory.trigger(this.props.taskId));
    });
  }
}

function mapHealthchecksToProps(t) {
  if (!t) return t;
  let hcs = t.healthcheckResults;
  t.hasSuccessfulHealthcheck = hcs && hcs.length > 0 && !!_.find(hcs, (h) => h.statusCode == 200);
  t.lastHealthcheckFailed = hcs && hcs.length > 0 && hcs[0].statusCode != 200;
  t.healthcheckFailureReasonMessage = Utils.healthcheckFailureReasonMessage(t);
  t.tooManyRetries = hcs && hcs.length > t.task.taskRequest.deploy.healthcheckMaxRetries && t.task.taskRequest.deploy.healthcheckMaxRetries > 0;
  t.secondsElapsed = t.taskRequest && t.taskRequest.deploy.healthcheckMaxTotalTimeoutSeconds ? t.taskRequest.deploy.healthcheckMaxTotalTimeoutSeconds : config.defaultDeployHealthTimeoutSeconds;
  return t;
}

function mapTaskToProps(t) {
  t.lastKnownState = _.last(t.taskUpdates);
   let isStillRunning = true;
   if (t.taskUpdates && _.contains(Utils.TERMINAL_TASK_STATES, t.lastKnownState.taskState)) {
     isStillRunning = false;
   }
   t.isStillRunning = isStillRunning;

   t.isCleaning = t.lastKnownState.taskState == 'TASK_CLEANING';

   let ports = [];
   if (t.task.taskRequest.deploy.resources.numPorts > 0) {
     for (let resource of t.task.mesosTask.resources) {
       if (resource.name == 'ports') {
         for (let range of resource.ranges.range) {
           for (let port of Utils.range(range.begin, range.end + 1)) {
             ports.push(port);
           }
         }
       }
     }
   }
   t.ports = ports;

   return t;
}

function mapStateToProps(state, ownProps) {
  let task = mapHealthchecksToProps(state.api.task[ownProps.taskId].data);
  task = mapTaskToProps(task);
  return {
    task: task,
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

export default connect(mapStateToProps)(TaskDetail);
