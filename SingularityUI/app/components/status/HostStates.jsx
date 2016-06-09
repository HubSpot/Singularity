import React from 'react';
import Table from '../common/Table';
import PlainText from '../common/atomicDisplayItems/PlainText';

export default class HostStates extends React.Component {

  getTableHeaders() {
    return ([
      { data: "Hostname" },
      { data: "Connected" },
      { data: "Uptime" },
      { data: "Time since last offer" }
    ]);
  }

  getTableRows(hosts) {
    if (hosts) {
      return hosts.map((h) => {
        return {
          dataId: h.hostname,
          data: [{
            component: PlainText,
            prop: {
                text: h.hostname
            }
        }]
        };
      });
    }
  }

  render() {
    console.log(this.props);
    return (
      <div>
          <h2>Singularity scheduler instances</h2>
          <Table
            noPages
            columnHeads={this.getTableHeaders()}
            tableRows={this.getTableRows(this.props.hosts) || []}
            />
      </div>
    );
  }
}

HostStates.propTypes = {};
