import React from 'react';
import Utils from '../../utils';

import Breadcrumbs from '../common/Breadcrumbs';
import SimpleTable from '../common/SimpleTable';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default class TaskFileBrowser extends React.Component {

  navigateTo(link) {
    app.router.navigate(Utils.joinPath(`#task/${this.props.taskId}/files/`, link), {trigger: true});
  }

  render() {
    const headers = ['Name', 'Size', 'Last Modified', ''];
    let pathItems = [];
    pathItems.push({
      text: "root",
      onClick: () => this.navigateTo('')
    });

    let p = '';
    let links = {};
    for (let s of _.without(this.props.files.currentDirectory.split('/'), '')) {
      p += `${s}`;
      links[s] = p;
      pathItems.push({
        text: s,
        onClick: () => this.navigateTo(links[s])
      })
      p += '/';
    }
    pathItems[pathItems.length - 1].onClick = null;

    return (
      <div>
        <Breadcrumbs items={pathItems} />
        <SimpleTable
          emptyMessage="No files exist in this directory"
          entries={_.sortBy(this.props.files.files, 'isDirectory').reverse()}
          perPage={10}
          renderTableHeaders={() => {
            let row = headers.map((h, i) => {
              return <th key={i}>{h}</th>;
            });
            return <tr>{row}</tr>;
          }}
          renderTableRow={(data, index) => {
            let nameLink = "";
            let icon = <Glyphicon iconClass={data.isDirectory ? 'folder-open' : 'file'} />;
            if (data.isTailable) {
              nameLink = <a href={`${config.appRoot}/task/${this.props.taskId}/tail/${data.uiPath}`}>{icon}<span className="file-name">{data.name}</span></a>;
            } else if (!data.isTailable && !data.isDirectory) {
              nameLink = <span>{icon} {data.name}</span>;
            } else {
              nameLink = <a onClick={() => this.navigateTo(`${this.props.files.currentDirectory}/${data.name}`)}>{icon}<span className="file-name">{data.name}</span></a>;
            }
            return (
              <tr key={index}>
                <td>{nameLink}</td>
                <td>{Utils.humanizeFileSize(data.size)}</td>
                <td>{Utils.absoluteTimestamp(data.mtime * 1000)}</td>
              </tr>
            );
          }}
        />
      </div>
    );
  }
}
