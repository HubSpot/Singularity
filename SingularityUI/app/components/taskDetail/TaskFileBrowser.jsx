import React, { PropTypes } from 'react';
import Utils from '../../utils';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { Glyphicon } from 'react-bootstrap';

import Breadcrumbs from '../common/Breadcrumbs';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';
import { Link } from 'react-router';

function TaskFileBrowser (props) {
  let pathItems = [];
  pathItems.push({
    text: 'root',
    onClick: () => props.changeDir('')
  });

  let pathSoFar = '';
  const links = {};
  for (const pathItem of _.without(props.currentDirectory.split('/'), '')) {
    pathSoFar += pathItem;
    links[pathItem] = pathSoFar;
    pathItems.push({
      text: pathItem,
      onClick: () => props.changeDir(links[pathItem])
    });
    pathSoFar += '/';
  }
  pathItems[pathItems.length - 1].onClick = null;

  function getFiles() {
    return _.sortBy(props.files, 'isDirectory').reverse();
  }

  return (
    <div>
      <Breadcrumbs items={pathItems} />
      <UITable
        data={getFiles() || []}
        paginated={true}
        rowChunkSize={10}
        keyGetter={(file) => file.name}
        emptyTableMessage="No files exist in this directory"
      >
        <Column
          label="Name"
          id="name"
          key="name"
          cellData={(file) => {
            const icon = <Glyphicon glyph={file.isDirectory ? 'folder-open' : 'file'} />;
            if (file.isTailable) {
              return <Link to={`task/${props.taskId}/tail/${file.uiPath}`}>{icon}<span className="file-name">{file.name}</span></Link>;
            }
            if (!file.isTailable && !file.isDirectory) {
              return <span>{icon} {file.name}</span>;
            }
            return <a onClick={() => props.changeDir(`${props.currentDirectory}/${file.name}`)}>{icon}<span className="file-name">{file.name}</span></a>;
          }}
          sortable={true}
        />
        <Column
          label="Size"
          id="size"
          key="size"
          cellData={(file) => Utils.humanizeFileSize(file.size)}
          sortable={true}
        />
        <Column
          label="Last Modified"
          id="last-modified"
          key="last-modified"
          cellData={(file) => Utils.absoluteTimestamp(file.mtime * 1000)}
          sortable={true}
        />
        <Column
          label=""
          id="actions-column"
          key="actions-column"
          cellData={(file) => !file.isDirectory && (
            <OverlayTrigger placement="left" overlay={<ToolTip id={`downloadFile${file.name}`}>Download {file.name}</ToolTip>}>
              <a href={file.downloadLink}>
                <Glyphicon glyph="download-alt" />
              </a>
            </OverlayTrigger>
          )}
        />
      </UITable>
    </div>
  );
}

TaskFileBrowser.propTypes = {
  files: PropTypes.arrayOf(PropTypes.shape({
    isDirectory: PropTypes.bool,
    isTailable: PropTypes.bool,
    name: PropTypes.string,
    downloadLink: PropTypes.string,
    size: PropTypes.number,
    mtime: PropTypes.number
  })),
  currentDirectory: PropTypes.string.isRequired,
  changeDir: PropTypes.func.isRequired,
  taskId: PropTypes.string.isRequired
};

export default TaskFileBrowser;
