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
  let alerts = [];

  for(let i in t.taskMetadata) {
    let md = t.taskMetadata[i];
    const message = md.message && (
      <pre className='pre-scrollable'>{md.message}</pre>
    );
    alerts.push(
      <Alert key={i} bsStyle={md.level == 'ERROR' ? 'danger' : 'warning'}>
        <h4>{md.title}</h4>
        <p>
          <strong>{Utils.timeStampFromNow(md.timestamp)}</strong> | Type: {md.type} {md.user ? `| User: ${md.user}` : null}
        </p>
        {message}
      </Alert>
    )
  }

  return (
    <div>
      {alerts}
    </div>
  );
}
