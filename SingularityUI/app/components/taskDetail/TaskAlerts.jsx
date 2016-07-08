import React, { PropTypes } from 'react';
import Utils from '../../utils';
import { Alert } from 'react-bootstrap';

import JSONButton from '../common/JSONButton';
import SimpleTable from '../common/SimpleTable';

const TaskAlerts = (props) => {
  let alerts = [];

  if (props.deploy.deployResult && props.deploy.deployResult.deployState === 'FAILED') {
    // Did this task cause a deploy to fail?
    if (Utils.isCauseOfFailure(props.task, props.deploy)) {
      alerts.push(
        <Alert key="failure" bsStyle="danger">
          <p>This contributed to the failure of <a href={`${config.appRoot}/request/${props.deploy.requestId}/deploy/${props.deploy.deployId}`}>
            Deploy {props.deploy.deployId}
          </a> because <strong>{Utils.causeOfDeployFailure(props.task, props.deploy)}</strong>.</p>
        </Alert>
      );
    } else {
      // Did a deploy cause this task to fail?
      const fails = props.deploy.deployResult.deployFailures.map((fail, key) => {
        if (fail.taskId) {
          return <li key={key}><a href={`${config.appRoot}/task/${fail.taskId.id}`}>{fail.taskId.id}</a>: {Utils.humanizeText(fail.reason)} {fail.message}</li>;
        }
        return <li key={key}>{Utils.humanizeText(fail.reason)} {fail.message}</li>;
      });
      alerts.push(
        <Alert key="failure" bsStyle="danger">
          <a href={`${config.appRoot}/request/${props.deploy.deploy.requestId}/deploy/${props.deploy.deploy.id}`}>Deploy {props.deploy.deploy.id} </a>failed.
          {Utils.ifDeployFailureCausedTaskToBeKilled(props.task) && ' This task was killed as a result of the failing deploy. '}
          {props.deploy.deployResult.deployFailures.length && ' The deploy failure was caused by: '}
          <ul>{fails}</ul>
        </Alert>
      );
    }
  }

  // Is this a scheduled task that has been running much longer than previous ones?
  if (props.task.isStillRunning && props.task.task.taskRequest.request.requestType === 'SCHEDULED' && props.deploy.deployStatistics) {
    const avg = props.deploy.deployStatistics.averageRuntimeMillis;
    const current = new Date().getTime() - props.task.task.taskId.startedAt;
    let threshold = config.warnIfScheduledJobIsRunningPastNextRunPct / 100;
    if (current > (avg * threshold)) {
      alerts.push(
        <Alert key="runLong" bsStyle="warning">
          <strong>Warning: </strong>
          This scheduled task has been running longer than <code>{threshold}</code> times the average for the request and may be stuck.
        </Alert>
      );
    }
  }

  // Was this task killed by a decomissioning slave?
  if (!props.task.isStillRunning) {
    const decomMessage = _.find(props.task.taskUpdates, (update) => {
      return update.statusMessage && update.statusMessage.indexOf('DECOMISSIONING') !== -1 && update.taskState === 'TASK_CLEANING';
    });
    const killedMessage = _.find(props.task.taskUpdates, (update) => {
      return update.taskState === 'TASK_KILLED';
    });
    if (decomMessage && killedMessage) {
      alerts.push(
        <Alert key="decom" bsStyle="warning">This task was replaced then killed by Singularity due to a slave decommissioning.</Alert>
      );
    }
  }

  // Healthcheck notification
  if (_.find(props.pendingDeploys, (pendingDeploy) =>
    pendingDeploy.deployMarker.requestId === props.task.task.taskId.requestId && pendingDeploy.deployMarker.deployId === props.task.task.taskId.deployId && pendingDeploy.currentDeployState === 'WAITING'
  )) {
    const hcTable = props.task.healthcheckResults.length > 0 && (
      <SimpleTable
        emptyMessage="No healthchecks"
        entries={[props.task.healthcheckResults[0]]}
        perPage={5}
        first={true}
        last={true}
        headers={['Timestamp', 'Duration', 'Status', 'Message']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index}>
              <td>{Utils.absoluteTimestamp(data.timestamp)}</td>
              <td>{data.durationMillis} {data.durationMillis && 'ms'}</td>
              <td>{data.statusCode ? <span className={`label label-${data.statusCode === 200 ? 'success' : 'danger'}`}>HTTP {data.statusCode}</span> : <span className="label label-warning">No Response</span>}</td>
              <td><pre className="healthcheck-message">{data.errorMessage || data.responseBody}</pre></td>
              <td className="actions-column"><JSONButton object={data}>{'{ }'}</JSONButton></td>
            </tr>
          );
        }}
      />
    );
    const pending = <span><strong>Deploy <code>{props.task.task.taskId.deployId}</code> is pending:</strong> Waiting for task to become healthy.</span>;
    alerts.push(
      <Alert key="hc" bsStyle="warning">
        <strong>Deploy <code>{props.task.task.taskId.deployId}</code> is pending: </strong>
        {props.task.hasSuccessfulHealthcheck && 'Waiting for successful load balancer update' || (props.task.healthcheckResults.length > 0 ? hcTable : pending)}
      </Alert>
    );
  }

  // Killed due to HC fail
  if (props.task.lastHealthcheckFailed && !props.task.isStillRunning) {
    alerts.push(
      <Alert key="hcFail" bsStyle="danger">
        <strong>Task killed due to no passing healthchecks after {props.task.tooManyRetries ? `${props.task.healthcheckResults.length.toString()} tries. ` : `${props.task.secondsElapsed.toString()} seconds. `}</strong>
        Last healthcheck {props.task.healthcheckResults[0].statusCode ?
          <span>responded with <span className="label label-danger">HTTP {props.task.healthcheckResults[0].statusCode}</span></span> :
          <span>did not respond after <code>{props.task.healthcheckResults[0].durationMillis && `${props.task.healthcheckResults[0].durationMillis.toString()} ms`}</code> at {Utils.absoluteTimestamp(props.task.healthcheckResults[0].timestamp)}</span>}
        <a href="#healthchecks"> View all healthchecks</a>
        <a href="#logs"> View service logs</a>
        {props.task.healthcheckFailureReasonMessage && <p>The healthcheck failed because {props.task.healthcheckFailureReasonMessage}</p>}
      </Alert>
    );
  }

  return (
    <div>
      {alerts}
    </div>
  );
};

TaskAlerts.propTypes = {
  deploy: PropTypes.shape({
    deployResult: PropTypes.shape({
      deployState: PropTypes.string,
      deployFailures: PropTypes.arrayOf(PropTypes.shape({
        reason: PropTypes.string,
        message: PropTypes.string,
        taskId: PropTypes.shape({
          id: PropTypes.string
        })
      }))
    }),
    deploy: PropTypes.shape({
      requestId: PropTypes.string,
      id: PropTypes.string
    }).isRequired,
    deployStatistics: PropTypes.shape({
      averageRuntimeMillis: PropTypes.number
    }),
    requestId: PropTypes.string,
    deployId: PropTypes.string
  }).isRequired,

  task: PropTypes.shape({
    task: PropTypes.shape({
      taskRequest: PropTypes.shape({
        request: PropTypes.shape({
          requestType: PropTypes.string
        }).isRequired
      }).isRequired,
      taskId: PropTypes.shape({
        requestId: PropTypes.string,
        deployId: PropTypes.string,
        startedAt: PropTypes.number
      }).isRequired
    }).isRequired,
    taskUpdates: PropTypes.arrayOf(PropTypes.shape({
      taskState: PropTypes.string,
      statusMessage: PropTypes.arrayOf(PropTypes.string)
    })),
    healthcheckResults: PropTypes.arrayOf(PropTypes.shape({
      statusCode: PropTypes.number,
      durationMillis: PropTypes.number,
      timestamp: PropTypes.number
    })).isRequired,
    lastHealthcheckFailed: PropTypes.bool,
    isStillRunning: PropTypes.bool,
    tooManyRetries: PropTypes.bool,
    hasSuccessfulHealthcheck: PropTypes.bool,
    healthcheckFailureReasonMessage: PropTypes.string,
    secondsElapsed: PropTypes.number.isRequired
  }).isRequired,

  pendingDeploys: PropTypes.arrayOf(PropTypes.shape({
    deployMarker: PropTypes.shape({
      requestId: PropTypes.string,
      deployId: PropTypes.string
    }).isRequired,
    currentDeployState: PropTypes.string
  })),
};

export default TaskAlerts;
