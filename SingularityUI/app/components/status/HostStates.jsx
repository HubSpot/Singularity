import React, { PropTypes } from 'react';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import { Glyphicon } from 'react-bootstrap';
import Utils from '../../utils';

function getStatusTextColor(driverStatus) {
  switch (driverStatus) {
    case 'SUBSCRIBED': return 'color-success';
    case 'NOT_STARTED': return 'text-muted';
    default: return '';
  }
}

function HostStates(props) {
  return (
    <div>
      <h2>Singularity scheduler instances</h2>
      <UITable
        data={props.hosts || []}
        paginated={true}
        rowChunkSize={100}
        keyGetter={(host) => host.hostname}
      >
        <Column
          label="Hostname"
          id="hostname"
          key="hostname"
          cellData={(host) => host.hostname}
          sortable={true}
        />
        <Column
          label="Scheduler Client Status"
          id="driverStatus"
          key="driverStatus"
          cellData={(host) => host.driverStatus}
          className={(driverStatus) => getStatusTextColor(driverStatus)}
          cellRender={(driverStatus) => Utils.humanizeText(driverStatus)}
          sortable={true}
        />
        <Column
          label="Connected"
          id="mesosConnected"
          key="mesosConnected"
          cellData={(host) => host.driverStatus === 'SUBSCRIBED' && host.mesosConnected}
          cellRender={(mesosConnected) => (
            <Glyphicon
              className={mesosConnected ? 'color-success' : 'color-error'}
              glyph={mesosConnected ? 'ok' : 'remove'}
            />
          )}
          sortable={true}
        />
        <Column
          label="Uptime"
          id="uptime"
          key="uptime"
          cellData={(host) => host.uptime}
          cellRender={(uptime) => Utils.duration(uptime)}
          sortable={true}
        />
        <Column
          label="Time since last offer"
          id="millisSinceLastOffer"
          key="millisSinceLastOffer"
          cellData={(host) => host.millisSinceLastOffer}
          cellRender={(millisSinceLastOffer) => (millisSinceLastOffer ? Utils.duration(millisSinceLastOffer) : '—')}
          sortable={true}
        />
      </UITable>
    </div>
  );
}

HostStates.propTypes = {
  hosts: PropTypes.arrayOf(PropTypes.shape({
    hostname: PropTypes.string.isRequired,
    driverStatus: PropTypes.string.isRequired,
    mesosConnected: PropTypes.bool.isRequired,
    uptime: PropTypes.number.isRequired,
    millisSinceLastOffer: PropTypes.number
  }))
};

export default HostStates;
