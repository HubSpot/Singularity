import React, { PropTypes } from 'react';
import Utils from '../../utils';
import Section from '../common/Section';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import { Glyphicon } from 'react-bootstrap';

function TaskS3Logs (props) {
  return (
    <Section title="S3 Logs">
      <UITable
        emptyTableMessage="This task has no history yet"
        data={props.s3Files}
        keyGetter={(s3File) => s3File.key}
        rowChunkSize={5}
        paginated={true}
      >
        <Column
          label="Log file"
          id="log-file"
          key="log-file"
          cellData={(s3File) => (
            // <a className="long-link" href={`${props.taskId}/view/${s3File.key}`} title={s3File.key}>
            <a className="long-link" href={s3File.getUrl} target="_blank" title={s3File.key}>
              {Utils.trimS3File(s3File.key.substring(s3File.key.lastIndexOf('/') + 1), props.taskId)}
            </a>
          )}
        />
        <Column
          label="Size"
          id="size"
          key="size"
          cellData={(s3File) => Utils.humanizeFileSize(s3File.size)}
        />
        <Column
          label="Last modified"
          id="last-modified"
          key="last-modified"
          cellData={(s3File) => Utils.absoluteTimestamp(s3File.lastModified)}
        />
        <Column
          id="actions-column"
          key="actions-column"
          className="actions-column"
          cellData={(s3File) => (
            <a href={s3File.downloadUrl} target="_blank" title="Download">
              <Glyphicon glyph="download-alt" />
            </a>
          )}
        />
      </UITable>
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
