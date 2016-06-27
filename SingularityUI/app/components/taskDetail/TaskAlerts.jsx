import React from 'react';
import Utils from '../../utils';
import { InfoBox, UsageInfo } from '../common/statelessComponents';
import { Alert } from 'react-bootstrap';
import FormGroup from 'react-bootstrap/lib/FormGroup';

import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import ConfirmationDialog from '../common/ConfirmationDialog';
import CollapsableSection from '../common/CollapsableSection';
import SimpleTable from '../common/SimpleTable';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default (props) => {
  const t = props.task;
  const deploy = props.deploy;
  const pendingDeploys = props.pendingDeploys;
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
  if (t.isStillRunning && t.task.taskRequest.request.requestType == 'SCHEDULED' && deploy.deployStatistics) {
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

  // Healthcheck notification
  if (_.find(pendingDeploys, (d) => {
    d.deployMarker.requestId == t.task.taskId.requestId && d.deployMarker.deployId == t.task.taskId.deployId && d.currentDeployState == 'WAITING'
  })) {
    const hcTable = t.healthcheckResults > 0 && (
      <SimpleTable
        emptyMessage="No healthchecks"
        entries={[t.healthcheckResults[0]]}
        perPage={5}
        first
        last
        headers={['Timestamp', 'Duration', 'Status', 'Message']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index}>
              <td>{Utils.absoluteTimestamp(data.timestamp)}</td>
              <td>{data.durationMillis} {data.durationMillis ? 'ms' : ''}</td>
              <td>{data.statusCode ? <span className={`label label-${data.statusCode == 200 ? 'success' : 'danger'}`}>HTTP {data.statusCode}</span> : <span className="label label-warning">No Response</span>}</td>
              <td><pre className="healthcheck-message">{data.errorMessage || data.responseBody}</pre></td>
              <td className="actions-column"><JSONButton object={data}>{'{ }'}</JSONButton></td>
            </tr>
          );
        }}
      />
    );
    const pending = <span><strong>Deploy <code>{t.task.taskId.deployId}</code> is pending:</strong> Waiting for task to become healthy.</span>;
    alerts.push(
      <Alert key='hc' bsStyle='warning'>
        <strong>Deploy <code>{t.task.taskId.deployId}</code> is pending: </strong>
        {t.hasSuccessfulHealthcheck ? "Waiting for successful load balancer update" : (t.healthcheckResults > 0 ? hcTable : pending)}
      </Alert>
    );
  }

  // Killed due to HC fail
  if (t.lastHealthcheckFailed && !t.isStillRunning) {
    alerts.push(
      <Alert key='hcFail' bsStyle='danger'>
        <strong>Task killed due to no passing healthchecks after {t.tooManyRetries ? t.healthcheckResults.length.toString() + ' tries. ' : t.secondsElapsed.toString() + ' seconds. '}</strong>
        Last healthcheck {t.healthcheckResults[0].statusCode ?
          <span>responded with <span className="label label-danger">HTTP {t.healthcheckResults[0].statusCode}</span></span> :
            <span>did not respond after <code>{t.healthcheckResults[0].durationMillis ? t.healthcheckResults[0].durationMillis.toString() + ' ms' : ''}</code> at {Utils.absoluteTimestamp(t.healthcheckResults[0].timestamp)}</span>}
          <a href="#healthchecks"> View all healthchecks</a>
          <a href="#logs"> View service logs</a>
          {t.healthcheckFailureReasonMessage ? <p>The healthcheck failed because {t.healthcheckFailureReasonMessage}</p> : ''}
      </Alert>
    )
  }

  return (
    <div>
      {alerts}
    </div>
  );
}
