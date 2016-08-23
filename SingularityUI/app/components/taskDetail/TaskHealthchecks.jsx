import React, { PropTypes } from 'react';
import Utils from '../../utils';

import JSONButton from '../common/JSONButton';
import CollapsableSection from '../common/CollapsableSection';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';

function TaskHealthchecks (props) {
  const healthchecks = props.healthcheckResults;
  return healthchecks && (healthchecks.length !== 0) && (
    <CollapsableSection title="Healthchecks" id="healthchecks">
      <div className="well">
        <span>
          Beginning on <strong>Task running</strong>, hit
          <a className="healthcheck-link" target="_blank" href={`http://${props.task.offer.hostname}:${_.first(props.ports)}${props.task.taskRequest.deploy.healthcheckUri}`}>
            {props.task.taskRequest.deploy.healthcheckUri}
          </a>
          with a <strong>{props.task.taskRequest.deploy.healthcheckTimeoutSeconds || config.defaultHealthcheckTimeoutSeconds}</strong> second timeout
          every <strong>{props.task.taskRequest.deploy.healthcheckIntervalSeconds || config.defaultHealthcheckIntervalSeconds}</strong> second(s)
          until <strong>HTTP 200</strong> is recieved,
          <strong>{props.task.taskRequest.deploy.healthcheckMaxRetries}</strong> retries have failed,
          or <strong>{props.task.taskRequest.deploy.healthcheckMaxTotalTimeoutSeconds || config.defaultDeployHealthTimeoutSeconds}</strong> seconds have elapsed.
        </span>
      </div>
      <UITable
        emptyTableMessage="No healthchecks"
        data={healthchecks}
        rowChunkSize={5}
        paginated={true}
        keyGetter={(healthcheckResult) => healthcheckResult.timestamp}
      >
        <Column
          label="Timestamp"
          id="timestamp"
          key="timestamp"
          cellData={(healthcheckResult) => Utils.absoluteTimestamp(healthcheckResult.timestamp)}
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
    </CollapsableSection>
  ) || <div></div>;
}

TaskHealthchecks.propTypes = {
  task: PropTypes.shape({
    taskRequest: PropTypes.shape({
      deploy: PropTypes.shape({
        healthcheckUri: PropTypes.string,
        healthcheckTimeoutSeconds: PropTypes.number,
        healthcheckIntervalSeconds: PropTypes.number,
        healthcheckMaxRetries: PropTypes.number,
        healthcheckMaxTotalTimeoutSeconds: PropTypes.number
      }).isRequired
    }).isRequired,
    offer: PropTypes.shape({
      hostname: PropTypes.string
    }).isRequired,
  }).isRequired,
  healthcheckResults: PropTypes.arrayOf(PropTypes.shape({
    timestamp: PropTypes.number,
    durationMillis: PropTypes.number,
    statusCode: PropTypes.number,
    errorMessage: PropTypes.string,
    responseBody: PropTypes.string
  })),
  ports: PropTypes.arrayOf(PropTypes.number)
};

export default TaskHealthchecks;
