import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import Clipboard from 'clipboard';

import Utils from '../../utils';
import { Link } from 'react-router';
import { Glyphicon } from 'react-bootstrap';
import {
  FetchTaskHistory,
  FetchActiveTasksForDeploy,
  FetchTaskHistoryForDeploy,
  FetchDeployForRequest
} from '../../actions/api/history';

import { DeployState, InfoBox } from '../common/statelessComponents';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';
import SimpleTable from '../common/SimpleTable';
import ServerSideTable from '../common/ServerSideTable';
import CollapsableSection from '../common/CollapsableSection';

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
    params: PropTypes.object
  }

  componentDidMount() {
    return new Clipboard('.info-copyable');
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
    return (
      <header className="detail-header">
        <div className="row">
          <div className="col-md-12">
            <Breadcrumbs
              items={[
                {
                  label: 'Request',
                  text: deploy.deploy.requestId,
                  link: `request/${deploy.deploy.requestId}`
                },
                {
                  label: 'Deploy',
                  text: deploy.deploy.id
                }
              ]}
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
        <ServerSideTable
          emptyMessage="No tasks"
          entries={tasks || []}
          paginate={true}
          perPage={5}
          fetchAction={FetchTaskHistoryForDeploy}
          fetchParams={[deploy.deploy.requestId, deploy.deploy.id]}
          headers={['Name', 'Last State', 'Started', 'Updated', '', '']}
          renderTableRow={(data, index) => {
            return (
              <tr key={index}>
                <td><Link to={`task/${data.taskId.id}`}>{data.taskId.id}</Link></td>
                <td><span className={`label label-${Utils.getLabelClassFromTaskState(data.lastTaskState)}`}>{Utils.humanizeText(data.lastTaskState)}</span></td>
                <td>{Utils.timestampFromNow(data.taskId.startedAt)}</td>
                <td>{Utils.timestampFromNow(data.updatedAt)}</td>
                <td className="actions-column"><Link to={`request/${data.taskId.requestId}/tail/${config.finishedTaskLogPath}?taskIds=${data.taskId.id}`} title="Log"><Glyphicon glyph="file" /></Link></td>
                <td className="actions-column"><JSONButton object={data}>{'{ }'}</JSONButton></td>
              </tr>
            );
          }}
        />
    </div>
    );
  }

  renderInfo(deploy) {
    let stats = [];

    if (deploy.deployMarker.timestamp) {
      stats.push(<InfoBox key="initiated" copyableClassName="info-copyable" name="Initiated" value={Utils.timestampFromNow(deploy.deployMarker.timestamp)} />);
    }
    if (deploy.deployResult && deploy.deployResult.timestamp) {
      stats.push(<InfoBox key="completed" copyableClassName="info-copyable" name="Completed" value={Utils.timestampFromNow(deploy.deployResult.timestamp)} />);
    }
    if (deploy.deploy.executorData && deploy.deploy.executorData.cmd) {
      stats.push(<InfoBox key="cmd" copyableClassName="info-copyable" name="Command" value={deploy.deploy.executorData.cmd} />);
    }
    if (deploy.deploy.resources.cpus) {
      let value = `CPUs: ${deploy.deploy.resources.cpus} | Memory (Mb): ${deploy.deploy.resources.memoryMb} | Ports: ${deploy.deploy.resources.numPorts}`;
      stats.push(<InfoBox key="cpus" copyableClassName="info-copyable" name="Resources" value={value} />);
    }
    if (deploy.deploy.executorData && deploy.deploy.executorData.extraCmdLineArgs) {
      stats.push(<InfoBox key="args" copyableClassName="info-copyable" name="Extra Command Line Arguments" value={deploy.deploy.executorData.extraCmdLineArgsd} />);
    }

    for (let statistic in deploy.deployStatistics) {
      if (typeof deploy.deployStatistics[statistic] !== 'object') {
        let value = typeof deploy.deployStatistics[statistic] === 'string' ? Utils.humanizeText(deploy.deployStatistics[statistic]) : deploy.deployStatistics[statistic];
        stats.push(
          <InfoBox copyableClassName="info-copyable" key={statistic} name={Utils.humanizeCamelcase(statistic)} value={value} />
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
        <SimpleTable
          emptyMessage="No healthchecks"
          entries={_.values(healthchecks)}
          perPage={5}
          first={true}
          last={true}
          headers={['Task', 'Timestamp', 'Duration', 'Status', 'Message', '']}
          renderTableRow={(data, index) => {
            return (
              <tr key={index}>
                <td><Link to={`task/${data.taskId.id}`}>{data.taskId.id}</Link></td>
                <td>{Utils.absoluteTimestamp(data.timestamp)}</td>
                <td>{data.durationMillis} {data.durationMillis && 'ms'}</td>
                <td>{data.statusCode ? <span className={`label label-${data.statusCode === 200 ? 'success' : 'danger'}`}>HTTP {data.statusCode}</span> : <span className="label label-warning">No Response</span>}</td>
                <td><pre className="healthcheck-message">{data.errorMessage || data.responseBody}</pre></td>
                <td className="actions-column"><JSONButton object={data}>{'{ }'}</JSONButton></td>
              </tr>
            );
          }}
        />
      </CollapsableSection>
    );
  }

  render() {
    const { deploy, activeTasks, taskHistory, latestHealthchecks } = this.props;
    return (
      <div>
        {this.renderHeader(deploy)}
        {this.renderActiveTasks(deploy, activeTasks)}
        {this.renderTaskHistory(deploy, taskHistory)}
        {this.renderInfo(deploy)}
        {this.renderHealthchecks(deploy, latestHealthchecks)}
      </div>
    );
  }
}

function mapDispatchToProps(dispatch) {
  return {
    fetchDeployForRequest: (requestId, deployId) => dispatch(FetchDeployForRequest.trigger(requestId, deployId, true)),
    fetchActiveTasksForDeploy: (requestId, deployId) => dispatch(FetchActiveTasksForDeploy.trigger(requestId, deployId)),
    clearTaskHistoryForDeploy: () => dispatch(FetchTaskHistoryForDeploy.clearData()),
    fetchTaskHistoryForDeploy: (requestId, deployId, count, page) => dispatch(FetchTaskHistoryForDeploy.trigger(requestId, deployId, count, page)),
    fetchTaskHistory: (taskId) => dispatch(FetchTaskHistory.trigger(taskId))
  };
}

function mapStateToProps(state, ownProps) {
  let latestHealthchecks = _.mapObject(state.api.task, (val) => {
    if (val.data && val.data.healthcheckResults && val.data.healthcheckResults.length > 0) {
      return _.max(val.data.healthcheckResults, (hc) => {
        return hc.timestamp;
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
    latestHealthchecks
  };
}

let firstLoad = true;

function refresh(props) {
  const promises = [];
  promises.push(props.fetchDeployForRequest(props.params.requestId, props.params.deployId));
  promises.push(props.fetchActiveTasksForDeploy(props.params.requestId, props.params.deployId));
  promises.push(props.clearTaskHistoryForDeploy());
  if (firstLoad) {
    firstLoad = false;
    props.fetchTaskHistoryForDeploy(props.params.requestId, props.params.deployId, 5, 1);
  }

  const allPromises = Promise.all(promises);
  allPromises.then(() => {
    for (const t of props.route.store.getState().api.activeTasksForDeploy.data) {
      props.fetchTaskHistory(t.taskId.id);
    }
  });
  return allPromises;
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(DeployDetail, (props) => `Deploy ${props.params.deployId}`, refresh));
