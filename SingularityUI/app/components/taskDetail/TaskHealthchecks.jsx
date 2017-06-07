import React, { PropTypes } from 'react';
import Utils from '../../utils';

import JSONButton from '../common/JSONButton';
import CollapsableSection from '../common/CollapsableSection';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';

function TaskHealthchecks (props) {
  const healthchecks = props.healthcheckResults;
  const healthcheckOptions = props.task.taskRequest.deploy.healthcheck || {};

  let beginningOnMessage;
  if (healthcheckOptions.startupDelaySeconds) {
    beginningOnMessage = (
      <strong>{healthcheckOptions.startupDelaySeconds}s after Task enters running</strong>
    );
  } else {
    beginningOnMessage = (
      <strong>when Task enters running</strong>
    );
  }

  let retries;
  if (healthcheckOptions.maxRetries || config.defaultHealthcheckMaxRetries > 0) {
    retries = (
      <li><strong>{healthcheckOptions.maxRetries || config.defaultHealthcheckMaxRetries}</strong> retries have failed. <span className="glyphicon glyphicon-remove color-error"></span></li>
    );
  }

  let badStatusCodes;
  if (healthcheckOptions.failureStatusCodes) {
    badStatusCodes = (
      <li>Any of <strong>[{healthcheckOptions.failureStatusCodes.join(", ")}]</strong> is received <span className="glyphicon glyphicon-remove color-error"></span></li>
    );
  }

  return (healthchecks && healthcheckOptions.uri && (
    <CollapsableSection title="Healthchecks" id="healthchecks">
      <div className="well">
        <p>
          Beginning {beginningOnMessage}, wait a max of <strong>{healthcheckOptions.startupTimeoutSeconds || config.defaultStartupTimeoutSeconds}s</strong> for app to start responding, then hit
          <a className="healthcheck-link" target="_blank" href={`http://${props.task.offers[0].hostname}:${Utils.healthcheckPort(healthcheckOptions, props.ports)}${healthcheckOptions.uri}`}>
            {healthcheckOptions.uri}
          </a>
          with a <strong>{healthcheckOptions.responseTimeoutSeconds || config.defaultHealthcheckTimeoutSeconds}</strong> second timeout
          every <strong>{healthcheckOptions.intervalSeconds || config.defaultHealthcheckIntervalSeconds}</strong> second(s)
          until:
        </p>
        <ul>
          <li>
            <strong>HTTP 200</strong> is recieved <span className="glyphicon glyphicon-ok color-success"></span>
          </li>
          {retries}
          {badStatusCodes}
        </ul>
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
    </CollapsableSection>
  )) || <div></div>;
}

TaskHealthchecks.propTypes = {
  task: PropTypes.shape({
    taskRequest: PropTypes.shape({
      deploy: PropTypes.shape({
        healthcheck: PropTypes.shape({
          uri: PropTypes.string,
          portIndex: PropTypes.number,
          portNumber: PropTypes.number,
          protocol: PropTypes.string,
          startupTimeoutSeconds: PropTypes.number,
          startupDelaySeconds: PropTypes.number,
          startupIntervalSeconds: PropTypes.number,
          intervalSeconds: PropTypes.number,
          responseTimeoutSeconds: PropTypes.number,
          maxRetries: PropTypes.number,
          failureStatusCodes: PropTypes.arrayOf(PropTypes.number)
        })
      })
    }).isRequired,
    offers: PropTypes.arrayOf(PropTypes.shape({
      hostname: PropTypes.string
    })).isRequired,
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
