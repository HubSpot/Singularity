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
  const link = t.isStillRunning ? (
    <a href={`${config.appRoot}/task/${t.task.taskId.id}/tail/${Utils.substituteTaskId(config.runningTaskLogPath, t.task.taskId.id)}`} title="Log">
        <span><Glyphicon iconClass="file" /> {Utils.fileName(config.runningTaskLogPath)}</span>
    </a>
  ) : (
    <a href={`${config.appRoot}/task/${t.task.taskId.id}/tail/${Utils.substituteTaskId(config.finishedTaskLogPath, t.task.taskId.id)}`} title="Log">
        <span><Glyphicon iconClass="file" /> {Utils.fileName(config.finishedTaskLogPath)}</span>
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
