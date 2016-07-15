import React, { PropTypes } from 'react';
import Utils from '../../utils';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { Glyphicon } from 'react-bootstrap';

import Breadcrumbs from '../common/Breadcrumbs';
import SimpleTable from '../common/SimpleTable';
import { Link } from 'react-router';

function TaskFileBrowser (props) {
  function navigateTo(link) {
    props.changeDir(link);
  }

  let pathItems = [];
  pathItems.push({
    text: 'root',
    onClick: () => navigateTo('')
  });

  let pathSoFar = '';
  const links = {};
  for (const pathItem of _.without(props.currentDirectory.split('/'), '')) {
    pathSoFar += pathItem;
    links[pathItem] = pathSoFar;
    pathItems.push({
      text: pathItem,
      onClick: () => navigateTo(links[pathItem])
    });
    pathSoFar += '/';
  }
  pathItems[pathItems.length - 1].onClick = null;

  return (
    <div>
      <Breadcrumbs items={pathItems} />
      <SimpleTable
        emptyMessage="No files exist in this directory"
        entries={_.sortBy(props.files, 'isDirectory').reverse()}
        perPage={10}
        first={props.files.length >= 30}
        last={props.files.length >= 30}
        headers={['Name', 'Size', 'Last Modified', '']}
        renderTableRow={(data, index) => {
          let nameLink = '';
          let icon = <Glyphicon glyph={data.isDirectory ? 'folder-open' : 'file'} />;
          if (data.isTailable) {
            nameLink = <Link to={`${config.appRoot}/task/${props.taskId}/tail/${data.uiPath}`}>{icon}<span className="file-name">{data.name}</span></Link>;
          } else if (!data.isTailable && !data.isDirectory) {
            nameLink = <span>{icon} {data.name}</span>;
          } else {
            nameLink = <a onClick={() => navigateTo(`${props.currentDirectory}/${data.name}`)}>{icon}<span className="file-name">{data.name}</span></a>;
          }
          const link = !data.isDirectory && (
            <OverlayTrigger placement="left" overlay={<ToolTip id={`downloadFile${data.name}`}>Download {data.name}</ToolTip>}>
              <a href={data.downloadLink}>
                <Glyphicon glyph="download-alt" />
              </a>
            </OverlayTrigger>
          );
          return (
            <tr key={index}>
              <td>{nameLink}</td>
              <td>{Utils.humanizeFileSize(data.size)}</td>
              <td>{Utils.absoluteTimestamp(data.mtime * 1000)}</td>
              <td className="actions-column">
                {link}
              </td>
            </tr>
          );
        }}
      />
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
  taskId: PropTypes.string.isRequired
};

export default TaskFileBrowser;
