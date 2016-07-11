import React, { PropTypes } from 'react';
import Utils from '../../utils';
import { Alert } from 'react-bootstrap';

function TaskMetadataAlerts (props) {
  const alerts = props.task.taskMetadata.map((metadataItem, key) => {
    const message = metadataItem.message && (
      <pre className="pre-scrollable">{metadataItem.message}</pre>
    );
    return (
      <Alert key={key} bsStyle={metadataItem.level === 'ERROR' ? 'danger' : 'warning'}>
        <h4>{metadataItem.title}</h4>
        <p>
          <strong>{Utils.timeStampFromNow(metadataItem.timestamp)}</strong> | Type: {metadataItem.type} {metadataItem.user && `| User: ${metadataItem.user}`}
        </p>
        {message}
      </Alert>
    );
  });

  return (
    <div>
      {alerts}
    </div>
  );
}

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
