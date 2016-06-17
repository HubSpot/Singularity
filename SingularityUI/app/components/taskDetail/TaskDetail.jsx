import React from 'react';
import { connect } from 'react-redux';
import Utils from '../../utils';
import { FetchAction as TaskFilesFetchAction } from '../../actions/api/taskFiles';
import { InfoBox } from '../common/statelessComponents';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import CollapsableSection from '../common/CollapsableSection';
import SimpleTable from '../common/SimpleTable';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

import TaskFileBrowser from './TaskFileBrowser';

class TaskDetail extends React.Component {

  renderHeader(t, cleanup) {
    const taskState = t.taskUpdates ? (
      <div className="col-xs-6 task-state-header">
        <h3>
          <span>Instance {t.task.taskId.instanceNo} </span>
          <span className={`label label-${Utils.getLabelClassFromTaskState(_.last(t.taskUpdates).taskState)} task-state-header-label`}>
            {Utils.humanizeText(_.last(t.taskUpdates).taskState)} {cleanup ? `(${Utils.humanizeText(cleanup.cleanupType)})` : ''}
          </span>
        </h3>
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
            <InfoBox copyableClassName="info-copyable" name="Executor GUID" value={t.task.mesosTask.executor.executorId.value} />
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

  render() {
    let task = this.props.task[this.props.taskId].data;
    let cleanup = _.find(this.props.taskCleanups, (c) => {
      return c.taskId.id == this.props.taskId;
    });

    console.log(task);

    return (
      <div>
        {this.renderHeader(task, cleanup)}
        {this.renderHistory(task)}
        {this.renderLatestLog(task, this.props.files)}
        {this.renderFiles(task, this.props.files)}
        {this.renderLbUpdates(task)}
        {this.renderInfo(task)}
      </div>
    );
  }
}

function mapStateToProps(state) {
  let files = state.api.taskFiles.data;
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

  return {
    task: state.api.task,
    taskCleanups: state.api.taskCleanups.data,
    files: files
  };
}

export default connect(mapStateToProps)(TaskDetail);
