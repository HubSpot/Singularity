import React, { PropTypes } from 'react';
import Utils from '../../utils';
import { Alert } from 'react-bootstrap';

const TaskMetadataAlerts = (props) => {
  const alerts = [];

  for (const index in props.task.taskMetadata) {
    if (props.task.taskMetadata.hasOwnProperty(index)) {
      const metadataItem = props.task.taskMetadata[index];
      const message = metadataItem.message && (
        <pre className="pre-scrollable">{metadataItem.message}</pre>
      );
      alerts.push(
        <Alert key={index} bsStyle={metadataItem.level === 'ERROR' ? 'danger' : 'warning'}>
          <h4>{metadataItem.title}</h4>
          <p>
            <strong>{Utils.timestampFromNow(metadataItem.timestamp)}</strong> | Type: {metadataItem.type} {metadataItem.user ? `| User: ${metadataItem.user}` : null}
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
  task: PropTypes.shape({
    taskMetadata: PropTypes.arrayOf(PropTypes.shape({
      message: PropTypes.string,
      level: PropTypes.string.isRequired,
      title: PropTypes.string.isRequired,
      timestamp: PropTypes.number.isRequired,
      type: PropTypes.string.isRequired,
      user: PropTypes.string
    })).isRequired
  }).isRequired
};

export default TaskMetadataAlerts;
