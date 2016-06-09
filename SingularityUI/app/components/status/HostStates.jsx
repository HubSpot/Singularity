import React from 'react';
import Table from '../common/Table';
import PlainText from '../common/atomicDisplayItems/PlainText';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import Timestamp from '../common/atomicDisplayItems/Timestamp';
import Utils from '../../utils';

export default class HostStates extends React.Component {

  getTableHeaders() {
    return ([
      { data: "Hostname" },
      { data: "Driver status" },
      { data: "Connected" },
      { data: "Uptime" },
      { data: "Time since last offer" }
    ]);
  }

  getStatusTextColor(status) {
    switch(status) {
      case 'DRIVER_RUNNING': return 'color-success';
      case 'DRIVER_NOT_STARTED': return 'text-muted';
      default: return '';
    }
  }

  getTableRows(hosts) {
    if (hosts) {
      return hosts.map((h) => {
        return {
          dataId: h.hostname,
          data: [
            {
              component: PlainText,
              prop: {
                  text: h.hostname
              }
            },
            {
              component: PlainText,
              className: this.getStatusTextColor(h.driverStatus),
              prop: {
                  text: Utils.humanizeText(h.driverStatus)
              }
            },
            {
              component: Glyphicon,
              className: h.driverStatus == "DRIVER_RUNNING" && h.mesosConnected ? 'color-success': 'color-error',
              prop: {
                  iconClass: h.driverStatus == "DRIVER_RUNNING" && h.mesosConnected ? 'ok' : 'remove'
              }
            },
            {
              component: Timestamp,
              prop: {
                  timestamp: h.uptime,
                  display: 'duration'
              }
            },
            {
              component: Timestamp,
              prop: {
                  timestamp: h.millisSinceLastOffer,
                  display: 'duration'
              }
            },
          ]
        };
      });
    }
  }

  render() {
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
