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
    <Section title="Load Balancer Updates">
      <SimpleTable
        emptyMessage="No Load Balancer Info"
        entries={t.loadBalancerUpdates}
        perPage={5}
        headers={['Timestamp', 'Request Type', 'State', 'Message', '']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index}>
              <td>{Utils.absoluteTimestamp(data.timestamp)}</td>
              <td>{Utils.humanizeText(data.loadBalancerRequestId.requestType)}</td>
              <td>{Utils.humanizeText(data.loadBalancerState)}</td>
              <td>{data.message}</td>
              <td className="actions-column">
                <JSONButton object={data}>{'{ }'}</JSONButton>
              </td>
            </tr>
          );
        }}
      />
    </Section>
  );
}
