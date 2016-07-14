import React, { PropTypes } from 'react';
import Utils from '../../utils';
import Section from '../common/Section';
import SimpleTable from '../common/SimpleTable';
import { Glyphicon } from 'react-bootstrap';

function TaskS3Logs (props) {
  return (
    <Section title="S3 Logs">
      <SimpleTable
        emptyMessage="No S3 logs"
        entries={props.s3Files}
        perPage={5}
        headers={['Log file', 'Size', 'Last modified', '']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index}>
              <td>
                <a className="long-link" href={data.getUrl} target="_blank" title={data.key}>
                    {Utils.trimS3File(data.key.substring(data.key.lastIndexOf('/') + 1), props.taskId)}
                </a>
              </td>
              <td>{Utils.humanizeFileSize(data.size)}</td>
              <td>{Utils.absoluteTimestamp(data.lastModified)}</td>
              <td className="actions-column">
                <a href={data.getUrl} target="_blank" title="Download">
                  <Glyphicon glyph="download-alt" />
                </a>
              </td>
            </tr>
          );
        }}
      />
    </Section>
  );
}

TaskS3Logs.propTypes = {
  s3Files: PropTypes.arrayOf(PropTypes.shape({
    getUrl: PropTypes.string.isRequired,
    key: PropTypes.string.isRequired,
    size: PropTypes.number.isRequired,
    lastModified: PropTypes.number.isRequired
  })).isRequired,
  taskId: PropTypes.string.isRequired
};

export default TaskS3Logs;
