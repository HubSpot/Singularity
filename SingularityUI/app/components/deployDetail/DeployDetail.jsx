import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import rootComponent from '../../rootComponent';

import Utils from '../../utils';
import { Link } from 'react-router';
import { Glyphicon, Button } from 'react-bootstrap';
import { FetchTaskHistoryForDeploy } from '../../actions/api/history';
import { initialize, refresh } from '../../actions/ui/deployDetail';

import { DeployState, InfoBox } from '../common/statelessComponents';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import CollapsableSection from '../common/CollapsableSection';
import RedeployButton from '../common/modalButtons/RedeployButton';

import ActiveTasksTable from './ActiveTasksTable';

class DeployDetail extends React.Component {
  static propTypes = {
    location: PropTypes.shape({
      pathname: PropTypes.string.isRequired
    }).isRequired,
    dispatch: PropTypes.func,
    deploy: PropTypes.object,
    activeTasks: PropTypes.array,
    taskHistory: PropTypes.array,
    latestHealthchecks: PropTypes.array,
    fetchTaskHistoryForDeploy: PropTypes.func,
    params: PropTypes.object,
    isTaskHistoryFetching: PropTypes.bool,
    notFound: PropTypes.bool,
    group: PropTypes.object
  }

  renderHeader(deploy) {
    let message;
    if (deploy.deployResult && deploy.deployResult.message) {
      message = (
        <div className="row">
          <div className="col-md-12">
            <div className="well text-muted">
              {deploy.deployResult.message}
            </div>
          </div>
        </div>
      );
    }
    let failures;
    if (deploy.deployResult && deploy.deployResult.deployFailures) {
      let fails = [];
      let key = 0;
      for (const failure of deploy.deployResult.deployFailures) {
        fails.push(failure.taskId ?
          <Link key={key} to={`task/${failure.taskId.id}`} className="list-group-item">
            <strong>{failure.taskId.id}</strong>: {failure.reason} (Instance {failure.taskId.instanceNo}): {failure.message}
          </Link>
          :
          <li key={key} className="list-group-item">{failure.reason}: {failure.message}</li>
        );
        key++;
      }
      if (fails.length) {
        failures = (
          <div className="row">
            <div className="col-md-12">
              <div className="panel panel-danger">
                <div className="panel-heading text-muted">Deploy had {fails.length} failure{fails.length > 1 && 's'}:</div>
                <div className="panel-body">
                  {fails}
                </div>
              </div>
            </div>
          </div>
        );
      }
    }
    const breadcrumbs = [
      {
        label: 'Request',
        text: this.props.params.requestId,
        link: `request/${this.props.params.requestId}`
      },
      {
        label: 'Deploy',
        text: this.props.params.deployId
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
            />
          </div>
        </div>
        <div className="row">
          <div className="col-md-8">
            <h1>
              <span>{deploy.deploy.id}</span>
              <DeployState state={deploy.deployResult && deploy.deployResult.deployState || 'PENDING'} />
            </h1>
          </div>
          <div className="col-md-4 button-container">
            <RedeployButton requestId={deploy.deploy.requestId} deployId={deploy.deploy.id}>
              <Button bsStyle="primary">Redeploy</Button>
            </RedeployButton>
            <JSONButton object={deploy} linkClassName="btn btn-default">
              JSON
            </JSONButton>
          </div>
        </div>
        {failures || message}
      </header>
    );
  }

  renderActiveTasks(deploy) {
    return (
      <div>
        <div className="page-header">
          <h2>Active Tasks</h2>
        </div>
        <ActiveTasksTable deployId={deploy.id} />
      </div>
    );
  }

  renderTaskHistory(deploy, tasks) {
    return (
      <div>
        <div className="page-header">
          <h2>Task History</h2>
        </div>
        <UITable
          emptyTableMessage="No tasks"
          data={tasks || []}
          keyGetter={(task) => task.taskId.id}
          rowChunkSize={5}
          paginated={true}
          fetchDataFromApi={(page, numberPerPage) => this.props.fetchTaskHistoryForDeploy(this.props.params.requestId, this.props.params.deployId, numberPerPage, page)}
          isFetching={this.props.isTaskHistoryFetching}
        >
          <Column
            label="Name"
            id="url"
            key="url"
            cellData={(task) => (
              <Link to={`task/${task.taskId.id}`}>
                {task.taskId.id}
              </Link>
            )}
          />
          <Column
            label="State"
            id="state"
            key="state"
            cellData={(task) => (
              <span className={`label label-${Utils.getLabelClassFromTaskState(task.lastTaskState)}`}>
                {Utils.humanizeText(task.lastTaskState)}
              </span>
            )}
          />
          <Column
            label="Started"
            id="started"
            key="started"
            cellData={(task) => Utils.timestampFromNow(task.taskId.startedAt)}
          />
          <Column
            label="Updated"
            id="updated"
            key="updated"
            cellData={(task) => Utils.timestampFromNow(task.updatedAt)}
          />
          <Column
            id="actions-column"
            key="actions-column"
            className="actions-column"
            cellData={(task) => (
              <span>
                <Link to={`task/${task.taskId.id}/tail/${config.finishedTaskLogPath}`}>
                  <Glyphicon glyph="file" />
                </Link>
                <JSONButton object={task} showOverlay={true}>{'{ }'}</JSONButton>
              </span>
            )}
          />
        </UITable>
    </div>
    );
  }

  renderInfo(deploy) {
    let stats = [];

    if (deploy.deployMarker.timestamp) {
      stats.push(<InfoBox key="initiated" name="Initiated" value={Utils.timestampFromNow(deploy.deployMarker.timestamp)} />);
    }
    if (deploy.deployResult && deploy.deployResult.timestamp) {
      stats.push(<InfoBox key="completed" name="Completed" value={Utils.timestampFromNow(deploy.deployResult.timestamp)} />);
    }
    if (deploy.deploy.executorData && deploy.deploy.executorData.cmd) {
      stats.push(<InfoBox key="cmd"  name="Command" value={deploy.deploy.executorData.cmd} />);
    }
    if (deploy.deploy.resources && deploy.deploy.resources.cpus) {
      let value = `CPUs: ${deploy.deploy.resources.cpus} | Memory (Mb): ${deploy.deploy.resources.memoryMb} | Ports: ${deploy.deploy.resources.numPorts}`;
      stats.push(<InfoBox key="cpus" name="Resources" value={value} />);
    }
    if (deploy.deploy.executorData && !_.isEmpty(deploy.deploy.executorData.extraCmdLineArgs)) {
      stats.push(<InfoBox key="args" name="Extra Command Line Arguments" join=" " value={deploy.deploy.executorData.extraCmdLineArgsd} />);
    }

    for (let statistic in deploy.deployStatistics) {
      if (typeof deploy.deployStatistics[statistic] !== 'object') {
        let value = typeof deploy.deployStatistics[statistic] === 'string' ? Utils.humanizeText(deploy.deployStatistics[statistic]) : deploy.deployStatistics[statistic];
        stats.push(
          <InfoBox key={statistic} name={Utils.humanizeCamelcase(statistic)} value={value} />
        );
      }
    }
    return (
      <CollapsableSection title="Info" defaultExpanded={true}>
        <div className="row">
          <ul className="list-unstyled horizontal-description-list">
            {stats}
          </ul>
        </div>
      </CollapsableSection>
    );
  }

  renderHealthchecks(deploy, healthchecks) {
    if (healthchecks.length === 0) return <div></div>;
    return (
      <CollapsableSection title="Latest Healthchecks">
        <UITable
          emptyTableMessage="No healthchecks"
          rowChunkSize={5}
          paginated={true}
          keyGetter={(healthcheck) => healthcheck.timestamp}
          data={_.values(healthchecks)}
        >
          <Column
            label="Task"
            id="task"
            key="task"
            cellData={(healthcheck) => (
              <Link to={`task/${healthcheck.taskId.id}`}>
                {healthcheck.taskId.id}
              </Link>
            )}
          />
          <Column
            label="Timestamp"
            id="timestamp"
            key="timestamp"
            cellData={(healthcheck) => Utils.absoluteTimestamp(healthcheck.timestamp)}
          />
          <Column
            label="Duration"
            id="duration"
            key="duration"
            cellData={(healthcheck) => `${healthcheck.durationMillis} ${healthcheck.durationMillis && 'ms'}`}
          />
          <Column
            label="Status"
            id="status"
            key="status"
            cellData={(healthcheck) => (healthcheck.statusCode ?
              <span className={`label label-${healthcheck.statusCode === 200 ? 'success' : 'danger'}`}>
                HTTP {healthcheck.statusCode}
              </span> :
              <span className="label label-warning">
                No Response
              </span>
            )}
          />
          <Column
            label="Message"
            id="message"
            key="message"
            cellData={(healthcheck) => (
              <pre className="healthcheck-message">
                {healthcheck.errorMessage || healthcheck.responseBody}
              </pre>
            )}
          />
          <Column
            id="actions-column"
            key="actions-column"
            className="actions-column"
            cellData={(healthcheck) => <JSONButton object={healthcheck}>{'{ }'}</JSONButton>}
          />
        </UITable>
      </CollapsableSection>
    );
  }

  render() {
    const { deploy, activeTasks, taskHistory, latestHealthchecks } = this.props;
    const emptyMessage = !deploy.deploy && <div className="empty-table-message">Deploy data not found</div>;
    return (
      <div>
        {this.renderHeader(deploy)}
        {this.renderActiveTasks(deploy, activeTasks)}
        {this.renderTaskHistory(deploy, taskHistory)}
        {emptyMessage}
        {deploy.deploy && this.renderInfo(deploy)}
        {deploy.deploy && this.renderHealthchecks(deploy, latestHealthchecks)}
      </div>
    );
  }
}

function mapStateToProps(state, ownProps) {
  let latestHealthchecks = _.mapObject(state.api.task, (val) => {
    if (val.data && val.data.healthcheckResults && val.data.healthcheckResults.length > 0) {
      return _.max(val.data.healthcheckResults, (healthcheckResult) => {
        return healthcheckResult.timestamp;
      });
    }
    return undefined;
  });
  latestHealthchecks = _.without(latestHealthchecks, undefined);

  return {
    notFound: state.api.deploy.statusCode === 404,
    pathname: ownProps.location.pathname,
    deploy: state.api.deploy.data,
    taskHistory: state.api.taskHistoryForDeploy.data,
    isTaskHistoryFetching: state.api.taskHistoryForDeploy.isFetching,
    group: state.api.deploy.data.deploy && _.first(_.filter(state.api.requestGroups.data, (filterGroup) => _.contains(filterGroup.requestIds, state.api.deploy.data.deploy.requestId))),
    latestHealthchecks
  };
}


export default connect(mapStateToProps, (dispatch) => bindActionCreators({
  fetchTaskHistoryForDeploy: FetchTaskHistoryForDeploy.trigger,
}, dispatch))(rootComponent(DeployDetail, (props) => refresh(props.params.requestId, props.params.deployId), true, true, (props) => initialize(props.params.requestId, props.params.deployId)));
