import React, { PropTypes } from 'react';
import Utils from '../../utils';
import Section from '../common/Section';
import { Glyphicon } from 'react-bootstrap';

function TaskLatestLog (props) {
  const link = props.isStillRunning ? (
    <a href={`${config.appRoot}/task/${props.task.taskId.id}/tail/${Utils.substituteTaskId(config.runningTaskLogPath, props.task.taskId.id)}`} title="Log">
        <span><Glyphicon glyph="file" /> {Utils.fileName(config.runningTaskLogPath)}</span>
    </a>
  ) : (
    <a href={`${config.appRoot}/task/${props.task.taskId.id}/tail/${Utils.substituteTaskId(config.finishedTaskLogPath, props.task.taskId.id)}`} title="Log">
        <span><Glyphicon glyph="file" /> {Utils.fileName(config.finishedTaskLogPath)}</span>
    </a>
  );
  return (
    <Section title="Logs" id="logs">
      <div className="row">
        <div className="col-md-4">
          <h4>{link}</h4>
        </div>
      </div>
    </Section>
  );
}

TaskLatestLog.propTypes = {
  task: PropTypes.shape({
    taskId: PropTypes.shape({
      id: PropTypes.string
    }).isRequired
  }).isRequired,
  isStillRunning: PropTypes.bool
};

export default TaskLatestLog;
