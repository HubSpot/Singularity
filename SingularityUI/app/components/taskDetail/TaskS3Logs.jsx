import React, { PropTypes, Component } from 'react';
import Utils from '../../utils';
import Section from '../common/Section';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import { Glyphicon } from 'react-bootstrap';
import { groupBy } from 'underscore';

class TaskS3Logs extends Component {
  constructor(props) {
    super(props);
    this.state = {
      viewingGroup: null
    };
  }

  getFileType(s3File) {
    return s3File.key.split('/')[0];
  }

  renderTable(s3Files) {
    const { taskStartedAt, taskId } = this.props;
    return (
      <div>
        <h5>
          <a onClick={() => this.setState({ viewingGroup: null })}>
            <Glyphicon glyph="chevron-left" />
            <span className="file-name">Back</span>
          </a>
          <span className="file-name">{this.state.viewingGroup}</span>
        </h5>
        <UITable
          emptyTableMessage="This task has no history yet"
          data={s3Files}
          keyGetter={(s3File) => s3File.key}
          rowChunkSize={5}
          paginated={true}
        >
          <Column
            label="Log file"
            id="log-file"
            key="log-file"
            cellData={(s3File) => (
              <a className="long-link" href={s3File.getUrl} target="_blank" title={s3File.key}>
                {Utils.trimS3File(s3File.key.substring(s3File.key.lastIndexOf('/') + 1), taskId)}
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
            cellData={(s3File) => Utils.absoluteTimestampWithSeconds(s3File.lastModified)}
          />
          <Column
            label="Estimated Start Time"
            id="estimated-start"
            key="estimated-start"
            cellData={(s3File) => (s3File.startTime) ? Utils.absoluteTimestampWithSeconds(s3File.startTime) : Utils.absoluteTimestampWithSeconds(taskStartedAt)}
          />
          <Column
            label="Estimated End Time"
            id="estimtaed-end"
            key="estimtaed-end"
            cellData={(s3File) => (s3File.endTime) ? Utils.absoluteTimestampWithSeconds(s3File.endTime) : Utils.absoluteTimestampWithSeconds(s3File.lastModified)}
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
      </div>
    );
  }

  renderFolders(groups) {
    return (
      <ul style={{ listStyle: 'none', margin: '25px 0 50px' }}>
        {groups.map(group => (
          <li key={group} style={{ paddingBottom: '10px' }}>
            <a onClick={() => this.setState({ viewingGroup: group })}>
              <Glyphicon glyph="folder-open" />
              <span className="file-name">{group}</span>
            </a>
          </li>
        ))}
      </ul>
    );
  }

  render() {
    const { s3Files } = this.props;
    if (s3Files.data && !_.isEmpty(s3Files.data)) {
      const groupedFiles = groupBy(s3Files.data, this.getFileType);

      return (
        <Section title="S3 Logs">
          {this.state.viewingGroup
            ? this.renderTable(groupedFiles[this.state.viewingGroup])
            : this.renderFolders(Object.keys(groupedFiles))
          }
        </Section>
      );
    } else if (s3Files.error || s3Files.statusCode == 500) {
      return (
        <Section title="S3 Logs" subtitle="Error Fetching Logs from S3"></Section>
      );
    } else {
      return null;
    }
  }
}

TaskS3Logs.propTypes = {
  s3Files: PropTypes.shape({
    data: PropTypes.arrayOf(PropTypes.shape({
      getUrl: PropTypes.string.isRequired,
      key: PropTypes.string.isRequired,
      size: PropTypes.number.isRequired,
      lastModified: PropTypes.number.isRequired,
      startTime: PropTypes.number,
      endTime: PropTypes.number
    }))
  }).isRequired,
  taskId: PropTypes.string.isRequired,
  taskStartedAt: PropTypes.number.isRequired
};

export default TaskS3Logs;
