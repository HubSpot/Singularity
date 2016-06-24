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
  const healthchecks = props.task.healthcheckResults;
  if (!healthchecks || healthchecks.length == 0) return null;
  return (
    <CollapsableSection title="Healthchecks" id="healthchecks">
      <div className="well">
        <span>
          Beginning on <strong>Task running</strong>, hit
          <a className="healthcheck-link" target="_blank" href={`http://${t.task.offer.hostname}:${_.first(t.ports)}${t.task.taskRequest.deploy.healthcheckUri}`}>
            {t.task.taskRequest.deploy.healthcheckUri}
          </a>
          with a <strong>{t.task.taskRequest.deploy.healthcheckTimeoutSeconds || config.defaultHealthcheckTimeoutSeconds}</strong> second timeout
          every <strong>{t.task.taskRequest.deploy.healthcheckIntervalSeconds || config.defaultHealthcheckIntervalSeconds}</strong> second(s)
          until <strong>HTTP 200</strong> is recieved,
          <strong>{t.task.taskRequest.deploy.healthcheckMaxRetries}</strong> retries have failed,
          or <strong>{t.task.taskRequest.deploy.healthcheckMaxTotalTimeoutSeconds || config.defaultDeployHealthTimeoutSeconds}</strong> seconds have elapsed.
        </span>
      </div>
      <SimpleTable
        emptyMessage="No healthchecks"
        entries={healthchecks}
        perPage={5}
        first
        last
        headers={['Timestamp', 'Duration', 'Status', 'Message', '']}
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
    </CollapsableSection>
  );
}
