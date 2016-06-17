import React from 'react';
import { connect } from 'react-redux';
import Utils from '../../utils';
import { FetchAction as TaskFilesFetchAction } from '../../actions/api/taskFiles';
import { FetchAction as TaskResourceUsageFetchAction } from '../../actions/api/taskResourceUsage';
import { InfoBox, UsageInfo } from '../common/statelessComponents';
import { Alert } from 'react-bootstrap';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import CollapsableSection from '../common/CollapsableSection';
import SimpleTable from '../common/SimpleTable';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

import TaskFileBrowser from './TaskFileBrowser';

class TaskDetail extends React.Component {

  constructor() {
    super();
    this.state = {
      previousUsage: null
    }
  }

  componentDidMount() {
    // Get a second sample for CPU usage right away
    if (this.props.task[this.props.taskId].data.isStillRunning) {
      this.props.dispatch(TaskResourceUsageFetchAction.trigger(this.props.taskId));
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
    const taskState = t.taskUpdates ? (
      <div className="col-xs-6 task-state-header">
        <h1>
          <span>Instance {t.task.taskId.instanceNo} </span>
          <span className={`label label-${Utils.getLabelClassFromTaskState(_.last(t.taskUpdates).taskState)} task-state-header-label`}>
            {Utils.humanizeText(_.last(t.taskUpdates).taskState)} {cleanup ? `(${Utils.humanizeText(cleanup.cleanupType)})` : ''}
          </span>
        </h1>
      </div>
    ) : null;

    const removeBtn = t.isStillRunning ? (
      <a className="btn btn-danger">
        {cleanup ?
          (cleanup.isImmediate ? 'Destroy task' : 'Override cleanup') :
          (t.isCleaning ? 'Destroy task' : 'Kill Task')}
      </a>
    ) : null;

    const terminationAlert = t.isStillRunning && !cleanup && t.isCleaning ? (
      <div className="alert alert-warning" role="alert">
          <strong>Task is terminating:</strong> To issue a non-graceful termination (kill -term), click Destroy Task.
      </div>
    ) : null;

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
            <JSONButton object={t} linkClassName="btn btn-default" text="JSON" />
            {removeBtn}
          </div>
        </div>
        {terminationAlert}
      </header>
    );
  }

  renderAlerts(t, deploy) {
    let alerts = [];

    if (deploy.deployResult && deploy.deployResult.deployState == 'FAILED') {
      // Did this task cause a deploy to fail?
      if (Utils.isCauseOfFailure(t, deploy)) {
        alerts.push(
          <Alert key='failure' bsStyle='danger'>
            <p>This task casued <a href={`${config.appRoot}/request/${deploy.requestId}/deploy/${deploy.deployId}`}>
              Deploy {deploy.deployId}
            </a> to fail. Cause: {Utils.causeOfDeployFailure(t, deploy)}</p>
          </Alert>
        );
      } else {
        // Did a deploy cause this task to fail?
        const fails = deploy.deployResult.deployFailures.map((f, i) => {
          if (f.taskId) {
            return <li key={i}><a href={`${config.appRoot}/task/${f.taskId.id}`}>{f.taskId.id}</a>: {Utils.humanizeText(f.reason)} {f.message}</li>;
          } else {
            return <li key={i}>{Utils.humanizeText(f.reason)} {f.message}</li>;
          }
        });
        alerts.push(
          <Alert key='failure' bsStyle='danger'>
            <a href={`${config.appRoot}/request/${deploy.deploy.requestId}/deploy/${deploy.deploy.id}`}>Deploy {deploy.deploy.id} </a>failed.
            {Utils.ifDeployFailureCausedTaskToBeKilled(t) ? ' This task was killed as a result of the failing deploy. ' : ''}
            {deploy.deployResult.deployFailures.length ? ' The deploy failure was caused by: ' : ''}
            <ul>{fails}</ul>
          </Alert>
        );
      }
    }

    // Is this a scheduled task that has been running much longer than previous ones?
    if (t.isStillRunning && t.task.taskRequest.request.requestType == 'SCHEDULED') {
      let avg = deploy.deployStatistics.averageRuntimeMillis;
      let current = new Date().getTime() - t.task.taskId.startedAt;
      let threshold = config.warnIfScheduledJobIsRunningPastNextRunPct / 100;
      if (current > (avg * threshold)) {
        alerts.push(
          <Alert key='runLong' bsStyle='warning'>
            <strong>Warning: </strong>
            This scheduled task has been running longer than <code>{threshold}</code> times the average for the request and may be stuck.
          </Alert>
        );
      }
    }

    // Was this task killed by a decomissioning slave?
    if (!t.isStillRunning) {
      let decomMessage = _.find(t.taskUpdates, (u) => {
        return u.statusMessage && u.statusMessage.indexOf('DECOMISSIONING') != -1 && u.taskState == 'TASK_CLEANING';
      })
      let killedMessage = _.find(t.taskUpdates, (u) => {
        return u.taskState == 'TASK_KILLED';
      });
      if (decomMessage && killedMessage) {
        alerts.push(
          <Alert key='decom' bsStyle='warning'>This task was replaced then killed by Singularity due to a slave decommissioning.</Alert>
        );
      }
    }

    return (
      <div>
        {alerts}
      </div>
    )
  }

  renderHistory(t) {
    return (
      <Section title="History">
        <SimpleTable
          emptyMessage="This task has no history yet"
          entries={t.taskUpdates.concat().reverse()}
          perPage={5}
          headers={['Status', 'Message', 'Time']}
          renderTableRow={(data, index) => {
            return (
              <tr key={index} className={index == 0 ? 'medium-weight' : ''}>
                <td>{Utils.humanizeText(data.taskState)}</td>
                <td>{data.statusMessage ? data.statusMessage : '—'}</td>
                <td>{Utils.timeStampFromNow(data.timestamp)}</td>
              </tr>
            );
          }}
        />
      </Section>
    );
  }

  renderLatestLog(t, files) {
    const link = t.isStillRunning ? (
      <a href={`${config.appRoot}/task/${this.props.taskId}/tail/${Utils.substituteTaskId(config.runningTaskLogPath, this.props.taskId)}`} title="Log">
          <span><Glyphicon iconClass="file" /> {Utils.fileName(config.runningTaskLogPath)}</span>
      </a>
    ) : (
      <a href={`${config.appRoot}/task/${this.props.taskId}/tail/${Utils.substituteTaskId(config.finishedTaskLogPath, this.props.taskId)}`} title="Log">
          <span><Glyphicon iconClass="file" /> {Utils.fileName(config.finishedTaskLogPath)}</span>
      </a>
    );
    return (
      <Section title="Logs">
        <div className="row">
          <div className="col-md-4">
            <h4>{link}</h4>
          </div>
        </div>
      </Section>
    )
  }

  renderFiles(t, files) {
    if (_.isEmpty(files)) {
      return (
        <div className="empty-table-message">
            {'Could not retrieve files. The host of this task is likely offline or the directory has been cleaned up.'}
        </div>
      );
    }
    return (
      <Section title="Files">
        <TaskFileBrowser
          taskId={t.task.taskId.id}
          files={files}
          changeDir={(path) => {
            if (path.startsWith('/')) path = path.substring(1);
            this.props.dispatch(TaskFilesFetchAction.trigger(this.props.taskId,path));
            app.router.navigate(Utils.joinPath(`#task/${this.props.taskId}/files/`, path));
          }}
        />
      </Section>
    );
  }

  renderS3Logs(f, s3Files) {
    return (
      <Section title="S3 Logs">
        <SimpleTable
          emptyMessage="No S3 logs"
          entries={s3Files}
          perPage={5}
          headers={['Log file', 'Size', 'Last modified', '']}
          renderTableRow={(data, index) => {
            return (
              <tr key={index}>
                <td>
                  <a className="long-link" href={data.getUrl} target="_blank" title={data.key}>
                      {Utils.trimS3File(data.key.substring(data.key.lastIndexOf('/') + 1), this.props.taskId)}
                  </a>
                </td>
                <td>{Utils.humanizeFileSize(data.size)}</td>
                <td>{Utils.absoluteTimestamp(data.lastModified)}</td>
                <td className="actions-column">
                  <a href={data.getUrl} target="_blank" title="Download">
                    <Glyphicon iconClass="download-alt"></Glyphicon>
                  </a>
                </td>
              </tr>
            );
          }}
        />
      </Section>
    );
  }

  renderLbUpdates(t) {
    return (
      <Section title="Load Balancer Updates">
        <SimpleTable
          emptyMessage="No Load Balancer Info"
          entries={t.loadBalancerUpdates}
          perPage={5}
          headers={['Timestamp', 'Request Type', 'State', 'Message', '']}
          renderTableRow={(data, index) => {
            return (
              <tr key={index}>
                <td>{Utils.absoluteTimestamp(data.timestamp)}</td>
                <td>{Utils.humanizeText(data.loadBalancerRequestId.requestType)}</td>
                <td>{Utils.humanizeText(data.loadBalancerState)}</td>
                <td>{data.message}</td>
                <td className="actions-column">
                  <JSONButton object={data} text="{ }" />
                </td>
              </tr>
            );
          }}
        />
      </Section>
    );
  }

  renderInfo(t) {
    return (
      <Section title="Info">
        <div className="row">
          <ul className="list-unstyled horizontal-description-list">
            <InfoBox copyableClassName="info-copyable" name="Task ID" value={t.task.taskId.id} />
            <InfoBox copyableClassName="info-copyable" name="Directory" value={t.directory} />
            {t.task.mesosTask.executor ? <InfoBox copyableClassName="info-copyable" name="Executor GUID" value={t.task.mesosTask.executor.executorId.value} /> : null}
            <InfoBox copyableClassName="info-copyable" name="Hostname" value={t.task.offer.hostname} />
            <InfoBox copyableClassName="info-copyable" name="Ports" value={t.ports.toString()} />
            <InfoBox copyableClassName="info-copyable" name="Rack ID" value={t.task.rackId} />
            {t.task.taskRequest.deploy.executorData ? <InfoBox copyableClassName="info-copyable" name="Extra Cmd Line Arguments (for Deploy)" value={t.task.taskRequest.deploy.executorData.extraCmdLineArgs} /> : null}
            {t.task.taskRequest.pendingTask && t.task.taskRequest.pendingTask.cmdLineArgsList ? <InfoBox copyableClassName="info-copyable" name="Extra Cmd Line Arguments (for Task)" value={t.task.taskRequest.pendingTask.cmdLineArgsList} /> : null}
          </ul>
        </div>
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

    const exceedingWarning = cpuUsageExceeding ? (
      <span className="label label-danger">CPU usage > 110% allocated</span>
    ) : null;

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

  renderEnvVariables(t) {
    if (!t.task.mesosTask.executor) return null;
    let vars = [];
    for (let v of t.task.mesosTask.executor.command.environment.variables) {
      vars.push(<InfoBox key={v.name} copyableClassName="info-copyable" name={v.name} value={v.value} />);
    }

    return (
      <CollapsableSection title="Environment variables">
        <div className="row">
          <ul className="list-unstyled horizontal-description-list">
            {vars}
          </ul>
        </div>
      </CollapsableSection>
    );
  }

  renderHealthchecks(t) {
    let healthchecks = t.healthcheckResults;
    return (
      <CollapsableSection title="Healthchecks">
        <div className="well">
          <span>
            Beginning on <strong>Task running</strong>, hit
            <a className="healthcheck-link" target="_blank" href={`http://${t.task.offer.hostname}:${_.first(t.ports)}${t.task.taskRequest.deploy.healthcheckUri}`}>
              {t.task.taskRequest.deploy.healthcheckUri}
            </a>
            with a <strong>{t.task.taskRequest.deploy.healthcheckTimeoutSeconds || config.defaultHealthcheckTimeoutSeconds}</strong> second timeout
            every <strong>{t.task.taskRequest.deploy.healthcheckIntervalSeconds || config.defaultHealthcheckIntervalSeconds}</strong> second(s)
            until <strong>HTTP 200</strong> is recieved,
            <strong>{t.task.taskRequest.deploy.healthcheckMaxRetries}</strong> retries have failed,
            or <strong>{t.task.taskRequest.deploy.healthcheckMaxTotalTimeoutSeconds || config.defaultDeployHealthTimeoutSeconds}</strong> seconds have elapsed.
          </span>
        </div>
        <SimpleTable
          emptyMessage="No healthchecks"
          entries={healthchecks}
          perPage={5}
          first
          last
          headers={['Timestamp', 'Duration', 'Status', 'Message', '']}
          renderTableRow={(data, index) => {
            return (
              <tr key={index}>
                <td>{Utils.absoluteTimestamp(data.timestamp)}</td>
                <td>{data.durationMillis} {data.durationMillis ? 'ms' : ''}</td>
                <td>{data.statusCode ? <span className={`label label-${data.statusCode == 200 ? 'success' : 'danger'}`}>HTTP {data.statusCode}</span> : <span className="label label-warning">No Response</span>}</td>
                <td><pre className="healthcheck-message">{data.errorMessage || data.responseBody}</pre></td>
                <td className="actions-column"><JSONButton object={data} text="{ }" /></td>
              </tr>
            );
          }}
        />
      </CollapsableSection>
    );
  }

  renderShellCommands(t) {
    return (
      <CollapsableSection title="Shell commands">

      </CollapsableSection>
    )
  }

  render() {
    let task = this.props.task[this.props.taskId].data;
    let cleanup = _.find(this.props.taskCleanups, (c) => {
      return c.taskId.id == this.props.taskId;
    });

    // console.log(task, this.props.deploy);

    return (
      <div>
        {this.renderHeader(task, cleanup)}
        {this.renderAlerts(task, this.props.deploy)}
        {this.renderHistory(task)}
        {this.renderLatestLog(task, this.props.files)}
        {this.renderFiles(task, this.props.files)}
        {this.renderS3Logs(task, this.props.s3Logs)}
        {this.renderLbUpdates(task)}
        {this.renderInfo(task)}
        {this.renderResourceUsage(task, this.props.resourceUsage)}
        {this.renderEnvVariables(task)}
        {this.renderHealthchecks(task)}
        {this.renderShellCommands(task)}
      </div>
    );
  }
}

function mapStateToProps(state) {
  let files = state.api.taskFiles.data;
  if (files.files) {
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

  return {
    task: state.api.task,
    taskCleanups: state.api.taskCleanups.data,
    files: files,
    resourceUsage: state.api.taskResourceUsage.data,
    cpuTimestamp: state.api.taskResourceUsage.data.timestamp,
    s3Logs: state.api.taskS3Logs.data,
    deploy: state.api.deploy.data
  };
}

export default connect(mapStateToProps)(TaskDetail);
