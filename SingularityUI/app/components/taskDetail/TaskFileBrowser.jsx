import React, { PropTypes } from 'react';
import Utils from '../../utils';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { Glyphicon, Tooltip } from 'react-bootstrap';

import Breadcrumbs from '../common/Breadcrumbs';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';
import { Link } from 'react-router';

function makeComparator(attribute) {
  return (file1, file2) => {
    if (file1.isDirectory && !file2.isDirectory) {
      return -1;
    }
    if (file2.isDirectory && !file1.isDirectory) {
      return 1;
    }
    let property1;
    let property2;
    if (typeof file1[attribute] === 'string') {
      property1 = file1[attribute].trim();
    } else {
      property1 = file1[attribute];
    }
    if (typeof file2[attribute] === 'string') {
      property2 = file2[attribute].trim();
    } else {
      property2 = file2[attribute];
    }
    if (property1 === property2) {
      return 0;
    }
    return property1 > property2 ? 1 : -1;
  };
}

function sortData(cellData, file) {
  return file;
}

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

  const recentlyModifiedTooltip = <Tooltip id="tooltip">File is currently being written to</Tooltip>;

  return (
    <div>
      <Breadcrumbs items={pathItems} />
      <UITable
        data={getFiles() || []}
        keyGetter={(file) => file.name}
        rowClassName={({isRecentlyModified}) => { return isRecentlyModified ? 'bg-info-light' : null; }}
        rowChunkSize={50}
        paginated={true}
        emptyTableMessage="No files exist in this directory"
        defaultSortBy="name"
        striped={false}
      >
        <Column
          className="icon-column"
          label=""
          id="icon"
          key="icon"
          cellData={({isRecentlyModified}) => isRecentlyModified &&
            <OverlayTrigger placement="top" overlay={recentlyModifiedTooltip}><div className="page-loader loader-small loader-info" /></OverlayTrigger>
          }
        />
        <Column
          label="Name"
          id="name"
          key="name"
          cellData={(file) => {
            const icon = <Glyphicon glyph={file.isDirectory ? 'folder-open' : 'file'} />;
            if (file.isTailable) {
              return <Link to={`task/${props.taskId}/tail/${file.uiPath}`}>{icon}<span className="file-name">{file.name.trim()}</span></Link>;
            }
            if (!file.isTailable && !file.isDirectory) {
              return <span>{icon}<span className="file-name">{file.name.trim()}</span></span>;
            }
            return <a onClick={() => props.changeDir(`${props.currentDirectory}/${file.name}`)}>{icon}<span className="file-name">{file.name.trim()}</span></a>;
          }}
          sortable={true}
          sortFunc={makeComparator('name')}
          sortData={sortData}
        />
        <Column
          label="Size"
          id="size"
          key="size"
          cellData={(file) => Utils.humanizeFileSize(file.size)}
          sortable={true}
          sortFunc={makeComparator('size')}
          sortData={sortData}
        />
        <Column
          label="Last Modified"
          id="last-modified"
          key="last-modified"
          cellData={(file) => Utils.absoluteTimestamp(file.mtime * 1000)}
          sortable={true}
          sortFunc={makeComparator('mtime')}
          sortData={sortData}
        />
        <Column
          label=""
          id="actions-column"
          key="actions-column"
          className="actions-column"
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
