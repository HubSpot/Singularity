import React, { PropTypes } from 'react';
import Utils from '../../utils';
import { Alert } from 'react-bootstrap';
import { Link } from 'react-router';

import JSONButton from '../common/JSONButton';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';

const TaskAlerts = (props) => {
  let alerts = [];
  if (props.deploy.deployResult && props.deploy.deployResult.deployState === 'FAILED') {
    // Did this task cause a deploy to fail?
    if (Utils.isCauseOfFailure(props.task, props.deploy)) {
      alerts.push(
        <Alert key="failure" bsStyle="danger">
          <p>
            <strong>
              {Utils.causeOfDeployFailure(props.task, props.deploy)}.
            </strong>
          </p>
          <p>
            This
            {props.deploy.deployResult.deployFailures.length === 1 && ' caused ' || ' contributed to '}
            the failure of
            <Link to={`request/${props.deploy.deploy.requestId}/deploy/${props.deploy.deploy.id}`}>
              {' '}Deploy {props.deploy.deploy.id}
            </Link>
            .
          </p>
        </Alert>
      );
    } else {
      // Did a deploy cause this task to fail?
      alerts.push(
        <Alert key="failure" bsStyle="danger">
          {Utils.ifDeployFailureCausedTaskToBeKilled(props.task) && 'This task was killed because '}
          <Link to={`request/${props.deploy.deploy.requestId}/deploy/${props.deploy.deploy.id}`}>Deploy {props.deploy.deploy.id}</Link> failed.
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
          This scheduled task has been running longer than <code>{threshold}</code> times the average for the deploy and may be stuck.
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
      <UITable
        emptyTableMessage="No healthchecks"
        data={[props.task.healthcheckResults[0]]}
        rowChunkSize={5}
        paginated={true}
        keyGetter={(healthcheckResult) => healthcheckResult.timestamp}
      >
        <Column
          label="Timestamp"
          id="timestamp"
          key="timestamp"
          cellData={(healthcheckResult) => Utils.absoluteTimestampWithSeconds(healthcheckResult.timestamp)}
        />
        <Column
          label="Duration"
          id="duration"
          key="duration"
          cellData={(healthcheckResult) => `${healthcheckResult.durationMillis} ${healthcheckResult.durationMillis && 'ms'}`}
        />
        <Column
          label="Status"
          id="status"
          key="status"
          cellData={(healthcheckResult) => healthcheckResult.statusCode && <span className={`label label-${healthcheckResult.statusCode === 200 ? 'success' : 'danger'}`}>HTTP {healthcheckResult.statusCode}</span> || <span className="label label-warning">No Response</span>}
        />
        <Column
          label="Message"
          id="message"
          key="message"
          cellData={(healthcheckResult) => <pre className="healthcheck-message">{healthcheckResult.errorMessage || healthcheckResult.responseBody}</pre>}
        />
        <Column
          id="actions-column"
          key="actions-column"
          className="actions-column"
          cellData={(healthcheckResult) => <JSONButton object={healthcheckResult} showOverlay={true}>{'{ }'}</JSONButton>}
        />
      </UITable>
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
    const lastHealthcheck = _.last(props.task.healthcheckResults);
    const healthcheckOptions = props.task.task.taskRequest.deploy.healthcheck;

    let respondedMessage;
    if (lastHealthcheck.statusCode) {
      respondedMessage = (
        <p>
          Last healthcheck <span>responded with <span className="label label-danger"> HTTP {lastHealthcheck.statusCode}</span></span>
        </p>
      );
    } else if (lastHealthcheck.startup) {
      respondedMessage = (
        <p>
          The healthcheck failed because of a refused connection. It is possible your app did not start properly or was not listening on the anticipated port ({Utils.healthcheckPort(healthcheckOptions, props.task.ports)}). Please check the logs for more details.
        </p>
      );
    } else {
      respondedMessage = (
        <span>
          Last healthcheck did not respond after{' '}
          <code>
            {lastHealthcheck.durationMillis && `${Utils.millisecondsToSecondsRoundToTenth(lastHealthcheck.durationMillis)} seconds`}
          </code>
          {' '}at {Utils.absoluteTimestampWithSeconds(lastHealthcheck.timestamp)}
        </span>
      );
    }

    let taskKilledReason;
    if (lastHealthcheck.startup) {
      taskKilledReason = (
        <span>beacuse it did not respond to healthchecks within <strong>{(healthcheckOptions && healthcheckOptions.startupTimeoutSeconds) || config.defaultStartupTimeoutSeconds}s</strong></span>
      );
    } else if (lastHealthcheck.statusCode && healthcheckOptions && healthcheckOptions.failureStatusCodes && healthcheckOptions.failureStatusCodes.indexOf(lastHealthcheck.statusCode) !== -1) {
      taskKilledReason = (
        <span>due to bad status code <strong>{lastHealthcheck.statusCode}</strong></span>
      );
    } else {
      taskKilledReason = (
        <span>due to no passing healthchecks after <strong>{props.task.tooManyRetries ? ` ${props.task.healthcheckResults.length} attempts.` : ` ${Utils.healthcheckTimeout(healthcheckOptions)} seconds.`}</strong></span>
      );
    }

    alerts.push(
      <Alert key="hcFail" bsStyle="warning">
        <p>
          <strong>
            Task killed {taskKilledReason}
          </strong>
        </p>
        {respondedMessage}
        <p><li>
          <a href="#healthchecks">View all healthchecks</a>
        </li>
        <li>
          <a href="#logs">View service logs</a>
        </li></p>
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
    }),
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
      statusMessage: PropTypes.string
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
    healthcheckFailureReasonMessage: PropTypes.string
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
