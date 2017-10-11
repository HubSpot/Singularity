import React, { PropTypes } from 'react';
import Utils from '../../utils';
import { Glyphicon } from 'react-bootstrap';
import { Link } from 'react-router';

import Section from '../common/Section';
import TaskStatus from './TaskStatus';

const getLink = (status, taskId, files) => {
  const runningTaskLogFound = files.files && _.find(files.files, (file) => {
    return file.uiPath === Utils.substituteTaskId(config.runningTaskLogPath, taskId);
  });

  if (status === TaskStatus.RUNNING || runningTaskLogFound) {
    return (
      <Link to={Utils.tailerPath(taskId, config.runningTaskLogPath)} title="Log">
          <span><Glyphicon glyph="file" /> {Utils.fileName(config.runningTaskLogPath)}</span>
      </Link>
    );
  } else if (status === TaskStatus.STOPPED) {
    return (
      <Link to={Utils.tailerPath(taskId, config.finishedTaskLogPath)} title="Log">
          <span><Glyphicon glyph="file" /> {Utils.fileName(config.finishedTaskLogPath)}</span>
      </Link>
    );
  }

  return null;
};

function TaskLatestLog({status, taskId, files, available}) {
  const link = getLink(status, taskId, files);

  if (status === TaskStatus.NEVER_RAN || !available) {
    return null;
  } else {
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
}

TaskLatestLog.propTypes = {
  taskId: PropTypes.string.isRequired,
  files: PropTypes.object.isRequired,
  status: PropTypes.oneOf([TaskStatus.RUNNING, TaskStatus.STOPPED, TaskStatus.NEVER_RAN]),
  available: PropTypes.bool,
};

export default TaskLatestLog;
