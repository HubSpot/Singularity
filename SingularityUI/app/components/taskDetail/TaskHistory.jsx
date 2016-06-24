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
    <Section title="History">
      <SimpleTable
        emptyMessage="This task has no history yet"
        entries={t.taskUpdates.concat().reverse()}
        perPage={5}
        headers={['Status', 'Message', 'Time']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index} className={index == 0 ? 'medium-weight' : ''}>
              <td>{Utils.humanizeText(data.taskState)}</td>
              <td>{data.statusMessage ? data.statusMessage : '—'}</td>
              <td>{Utils.timeStampFromNow(data.timestamp)}</td>
            </tr>
          );
        }}
      />
    </Section>
  );
}
