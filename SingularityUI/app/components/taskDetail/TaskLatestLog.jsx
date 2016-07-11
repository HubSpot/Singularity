import React from 'react';
import Utils from '../../utils';
import { Link } from 'react-router';

import Section from '../common/Section';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default (props) => {
  const t = props.task;
  const link = t.isStillRunning ? (
    <Link to={`task/${t.task.taskId.id}/tail/${Utils.substituteTaskId(config.runningTaskLogPath, t.task.taskId.id)}`} title="Log">
        <span><Glyphicon iconClass="file" /> {Utils.fileName(config.runningTaskLogPath)}</span>
    </Link>
  ) : (
    <Link to={`task/${t.task.taskId.id}/tail/${Utils.substituteTaskId(config.finishedTaskLogPath, t.task.taskId.id)}`} title="Log">
        <span><Glyphicon iconClass="file" /> {Utils.fileName(config.finishedTaskLogPath)}</span>
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
};
