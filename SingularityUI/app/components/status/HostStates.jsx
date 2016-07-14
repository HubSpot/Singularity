import React, { PropTypes } from 'react';
import SimpleTable from '../common/SimpleTable';
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
      <SimpleTable
        emptyMessage="No hosts"
        entries={props.hosts || []}
        perPage={100}
        headers={['Hostname', 'Driver status', 'Connected', 'Uptime', 'Time since last offer']}
        renderTableRow={(host, key) => (
          <tr key={key}>
            <td>{host.hostname}</td>
            <td className={getStatusTextColor(host.driverStatus)}>
              {Utils.humanizeText(host.driverStatus)}
            </td>
            <td>
              <Glyphicon
                className={(host.driverStatus === 'DRIVER_RUNNING' && host.mesosConnected) ? 'color-success' : 'color-error'}
                glyph={(host.driverStatus === 'DRIVER_RUNNING' && host.mesosConnected) ? 'ok' : 'remove'}
              />
            </td>
            <td>{Utils.duration(host.uptime)}</td>
            <td>{Utils.duration(host.millisSinceLastOffer)}</td>
          </tr>
        )}
      />
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
