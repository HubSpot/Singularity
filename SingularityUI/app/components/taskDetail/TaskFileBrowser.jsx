import React, { PropTypes } from 'react';
import Utils from '../../utils';

import Breadcrumbs from '../common/Breadcrumbs';
import SimpleTable from '../common/SimpleTable';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import Link from '../common/atomicDisplayItems/Link';

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
  for (const pathItem of props.currentDirectory.split('/')) {
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
          let icon = <Glyphicon iconClass={data.isDirectory ? 'folder-open' : 'file'} />;
          if (data.isTailable) {
            nameLink = <a href={`${config.appRoot}/task/${props.taskId}/tail/${data.uiPath}`}>{icon}<span className="file-name">{data.name}</span></a>;
          } else if (!data.isTailable && !data.isDirectory) {
            nameLink = <span>{icon} {data.name}</span>;
          } else {
            nameLink = <a onClick={() => navigateTo(`${props.currentDirectory}/${data.name}`)}>{icon}<span className="file-name">{data.name}</span></a>;
          }
          let linkProps = {
            text: <Glyphicon iconClass="download-alt" />,
            url: data.downloadLink,
            title: 'Download',
            altText: `Download ${data.name}`,
            overlayTrigger: true,
            overlayTriggerPlacement: 'left',
            overlayToolTipContent: `Download ${data.name}`,
            overlayId: `downloadFile${data.name}`
          };
          const link = !data.isDirectory && <Link prop={linkProps} />;
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
  currentDirectory: PropTypes.string,
  taskId: PropTypes.string.isRequired
};

export default TaskFileBrowser;
