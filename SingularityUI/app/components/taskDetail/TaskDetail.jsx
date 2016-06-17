import React from 'react';
import { connect } from 'react-redux';
import Utils from '../../utils';
import { FetchAction as TaskFilesFetchAction } from '../../actions/api/taskFiles';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import CollapsableSection from '../common/CollapsableSection';
import SimpleTable from '../common/SimpleTable';

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
    const headers = ['Status', 'Message', 'Time'];
    return (
      <Section title="History">
        <SimpleTable
          emptyMessage="This task has no history yet"
          entries={t.taskUpdates.concat().reverse()}
          perPage={5}
          renderTableHeaders={() => {
            let row = headers.map((h, i) => {
              return <th key={i}>{h}</th>;
            });
            return <tr>{row}</tr>;
          }}
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

  render() {
    let task = this.props.task[this.props.taskId].data;
    let cleanup = _.find(this.props.taskCleanups, (c) => {
      return c.taskId.id == this.props.taskId;
    });

    // console.log(this.props.files);

    return (
      <div>
        {this.renderHeader(task, cleanup)}
        {this.renderHistory(task)}
        {this.renderFiles(task, this.props.files)}
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
    files: state.api.taskFiles.data
  };
}

export default connect(mapStateToProps)(TaskDetail);
