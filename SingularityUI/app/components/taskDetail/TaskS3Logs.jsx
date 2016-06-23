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
  const s3Files = props.s3Files;
  return (
    <Section title="S3 Logs">
      <SimpleTable
        emptyMessage="No S3 logs"
        entries={s3Files}
        perPage={5}
        headers={['Log file', 'Size', 'Last modified', '']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index}>
              <td>
                <a className="long-link" href={data.getUrl} target="_blank" title={data.key}>
                    {Utils.trimS3File(data.key.substring(data.key.lastIndexOf('/') + 1), t.task.taskId.id)}
                </a>
              </td>
              <td>{Utils.humanizeFileSize(data.size)}</td>
              <td>{Utils.absoluteTimestamp(data.lastModified)}</td>
              <td className="actions-column">
                <a href={data.getUrl} target="_blank" title="Download">
                  <Glyphicon iconClass="download-alt"></Glyphicon>
                </a>
              </td>
            </tr>
          );
        }}
      />
    </Section>
  );
}
