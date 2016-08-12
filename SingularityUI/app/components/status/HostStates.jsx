import React, { PropTypes } from 'react';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import { Glyphicon } from 'react-bootstrap';
import Utils from '../../utils';

function getStatusTextColor(status) {
  switch (status) {
    case 'DRIVER_RUNNING': return 'color-success';
    case 'DRIVER_NOT_STARTED': return 'text-muted';
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
        keyGetter={(h) => h.hostname}
      >
        <Column
          label="Hostname"
          id="hostname"
          key="hostname"
          cellData={(h) => h.hostname}
          sortable={true}
        />
        <Column
          label="Driver status"
          id="driverStatus"
          key="driverStatus"
          cellData={(h) => h.driverStatus}
          className={(s) => getStatusTextColor(s)}
          cellRender={(s) => Utils.humanizeText(s)}
          sortable={true}
        />
        <Column
          label="Connected"
          id="mesosConnected"
          key="mesosConnected"
          cellData={(h) => h.driverStatus === 'DRIVER_RUNNING' && h.mesosConnected}
          cellRender={(c) => (
            <Glyphicon
              className={c ? 'color-success' : 'color-error'}
              glyph={c ? 'ok' : 'remove'}
            />
          )}
          sortable={true}
        />
        <Column
          label="Uptime"
          id="uptime"
          key="uptime"
          cellData={(h) => h.uptime}
          cellRender={(u) => Utils.duration(u)}
          sortable={true}
        />
        <Column
          label="Time since last offer"
          id="millisSinceLastOffer"
          key="millisSinceLastOffer"
          cellData={(h) => h.millisSinceLastOffer}
          cellRender={(l) => (l ? Utils.duration(l) : '—')}
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
