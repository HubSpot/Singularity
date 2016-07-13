import React, { PropTypes } from 'react';

import { Alert } from 'react-bootstrap';

import Utils from '../../utils';

const TaskMetadataAlerts = (props) => {
  const t = props.task;
  const alerts = [];

  for (const i in t.taskMetadata) {
    if (t.taskMetadata.hasOwnProperty(i)) {
      const md = t.taskMetadata[i];
      const message = md.message && (
        <pre className="pre-scrollable">{md.message}</pre>
      );
      alerts.push(
        <Alert key={i} bsStyle={md.level === 'ERROR' ? 'danger' : 'warning'}>
          <h4>{md.title}</h4>
          <p>
            <strong>{Utils.timestampFromNow(md.timestamp)}</strong> | Type: {md.type} {md.user ? `| User: ${md.user}` : null}
          </p>
          {message}
        </Alert>
      );
    }
  }

  return (
    <div>
      {alerts}
    </div>
  );
};

TaskMetadataAlerts.propTypes = {
  task: PropTypes.object.isRequired
};

export default TaskMetadataAlerts;
