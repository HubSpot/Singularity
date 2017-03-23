import React, { PropTypes } from 'react';
import Utils from '../../utils';
import { Glyphicon } from 'react-bootstrap';
import { Link } from 'react-router';

import Section from '../common/Section';

function TaskLatestLog (props) {
  const link = props.isStillRunning ? (
    <Link to={Utils.tailerPath(props.taskId, config.runningTaskLogPath)} title="Log">
        <span><Glyphicon glyph="file" /> {Utils.fileName(config.runningTaskLogPath)}</span>
    </Link>
  ) : (
    <Link to={Utils.tailerPath(props.taskId, config.finishedTaskLogPath)} title="Log">
        <span><Glyphicon glyph="file" /> {Utils.fileName(config.finishedTaskLogPath)}</span>
    </Link>
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
  taskId: PropTypes.string.isRequired,
  isStillRunning: PropTypes.bool
};

export default TaskLatestLog;
