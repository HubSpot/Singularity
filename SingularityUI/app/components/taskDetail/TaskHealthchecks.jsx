import React, { PropTypes } from 'react';
import Utils from '../../utils';

import JSONButton from '../common/JSONButton';
import CollapsableSection from '../common/CollapsableSection';
import SimpleTable from '../common/SimpleTable';

function TaskHealthchecks (props) {
  const healthchecks = props.task.healthcheckResults;
  return healthchecks && (healthchecks.length !== 0) && (
    <CollapsableSection title="Healthchecks" id="healthchecks">
      <div className="well">
        <span>
          Beginning on <strong>Task running</strong>, hit
          <a className="healthcheck-link" target="_blank" href={`http://${props.task.task.offer.hostname}:${_.first(props.task.ports)}${props.task.task.taskRequest.deploy.healthcheckUri}`}>
            {props.task.task.taskRequest.deploy.healthcheckUri}
          </a>
          with a <strong>{props.task.task.taskRequest.deploy.healthcheckTimeoutSeconds || config.defaultHealthcheckTimeoutSeconds}</strong> second timeout
          every <strong>{props.task.task.taskRequest.deploy.healthcheckIntervalSeconds || config.defaultHealthcheckIntervalSeconds}</strong> second(s)
          until <strong>HTTP 200</strong> is recieved,
          <strong>{props.task.task.taskRequest.deploy.healthcheckMaxRetries}</strong> retries have failed,
          or <strong>{props.task.task.taskRequest.deploy.healthcheckMaxTotalTimeoutSeconds || config.defaultDeployHealthTimeoutSeconds}</strong> seconds have elapsed.
        </span>
      </div>
      <SimpleTable
        emptyMessage="No healthchecks"
        entries={healthchecks}
        perPage={5}
        first={true}
        last={true}
        headers={['Timestamp', 'Duration', 'Status', 'Message', '']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index}>
              <td>{Utils.absoluteTimestamp(data.timestamp)}</td>
              <td>{data.durationMillis} {data.durationMillis ? 'ms' : ''}</td>
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

TaskHealthchecks.propTypes = {
  task: PropTypes.shape({
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
  }).isRequired
};

export default TaskHealthchecks;
