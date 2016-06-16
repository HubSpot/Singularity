import React from 'react';
import Utils from '../../utils';

import Breadcrumbs from '../common/Breadcrumbs';
import SimpleTable from '../common/SimpleTable';

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
          entries={this.props.files.files}
          perPage={10}
          renderTableHeaders={() => {
            let row = headers.map((h, i) => {
              return <th key={i}>{h}</th>;
            });
            return <tr>{row}</tr>;
          }}
          renderTableRow={(data, index) => {
            let nameLink = "";
            if (data.isTailable) {
              nameLink = <a href={`${config.appRoot}/task/${this.props.taskId}/tail/${data.uiPath}`}>{data.name}</a>;
            } else if (!data.isTailable && !data.isDirectory) {
              nameLink = data.name;
            } else {
              nameLink = <a onClick={() => this.navigateTo(`${this.props.files.currentDirectory}/${data.name}`)}>{data.name}</a>;
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
