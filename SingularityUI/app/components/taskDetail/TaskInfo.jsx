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
  return (
    <Section title="Info">
      <div className="row">
        <ul className="list-unstyled horizontal-description-list">
          <InfoBox copyableClassName="info-copyable" name="Task ID" value={t.task.taskId.id} />
          <InfoBox copyableClassName="info-copyable" name="Directory" value={t.directory} />
          {t.task.mesosTask.executor ? <InfoBox copyableClassName="info-copyable" name="Executor GUID" value={t.task.mesosTask.executor.executorId.value} /> : null}
          <InfoBox copyableClassName="info-copyable" name="Hostname" value={t.task.offer.hostname} />
          <InfoBox copyableClassName="info-copyable" name="Ports" value={t.ports.toString()} />
          <InfoBox copyableClassName="info-copyable" name="Rack ID" value={t.task.rackId} />
          {t.task.taskRequest.deploy.executorData ? <InfoBox copyableClassName="info-copyable" name="Extra Cmd Line Arguments (for Deploy)" value={t.task.taskRequest.deploy.executorData.extraCmdLineArgs} /> : null}
          {t.task.taskRequest.pendingTask && t.task.taskRequest.pendingTask.cmdLineArgsList ? <InfoBox copyableClassName="info-copyable" name="Extra Cmd Line Arguments (for Task)" value={t.task.taskRequest.pendingTask.cmdLineArgsList} /> : null}
        </ul>
      </div>
    </Section>
  );
}
