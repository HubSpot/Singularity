import React, {PropTypes} from 'react';
import MachinesPage from './MachinesPage';
import {Glyphicon} from 'react-bootstrap';
import ModalButton from './ModalButton';
import MessageElement from './MessageElement';
import Utils from '../../utils';
import { connect } from 'react-redux';
import { FetchSlaves, FreezeSlave, DecommissionSlave, RemoveSlave, ReactivateSlave } from '../../actions/api/slaves';

function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}

class Slaves extends React.Component {

  static propTypes = {
    freezeSlave: PropTypes.func.isRequired,
    decommissionSlave: PropTypes.func.isRequired,
    removeSlave: PropTypes.func.isRequired,
    reactivateSlave: PropTypes.func.isRequired,
    clear: PropTypes.func.isRequired,
    error: PropTypes.string,
    slaves: PropTypes.arrayOf(PropTypes.shape({
      state: PropTypes.string
    }))
  }

  componentWillUnmount() {
    this.props.clear();
  }

  showUser(slave) {
    return __in__(slave.currentState.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']);
  }

  getMaybeReactivateButton(slave) {
    return (__in__(slave.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']) &&
      <ModalButton
        buttonChildren={<Glyphicon glyph="new-window" />}
        action="Reactivate Slave"
        onConfirm={(data) => this.props.reactivateSlave(slave, data.message)}
        tooltipText={`Reactivate ${slave.id}`}
        formElements={[MessageElement]}>
        <p>Are you sure you want to cancel decommission and reactivate this slave??</p>
        <pre>{slave.id}</pre>
        <p>Reactivating a slave will cancel the decommission without erasing the slave's history and move it back to the active state.</p>
      </ModalButton>
    );
  }

  getMaybeFreezeButton(slave) {
    return (slave.currentState.state === 'ACTIVE' &&
      <ModalButton
        buttonChildren={<Glyphicon glyph="stop" />}
        action="Freeze Slave"
        onConfirm={(data) => this.props.freezeSlave(slave, data.message)}
        tooltipText={`Freeze ${slave.id}`}
        formElements={[MessageElement]}>
        <p>Are you sure you want to freeze this slave?</p>
        <pre>{slave.id}</pre>
        <p>Freezing a slave will prevent new tasks from being launched. Previously running tasks will be unaffected.</p>
      </ModalButton>
    );
  }

  getDecommissionOrRemoveButton(slave) {
    if (__in__(slave.currentState.state, ['ACTIVE', 'FROZEN'])) {
      return (
        <ModalButton
          buttonChildren={<Glyphicon glyph="trash" />}
          action="Decommission Slave"
          onConfirm={(data) => this.props.decommissionSlave(slave, data.message)}
          tooltipText={`Decommission ${slave.id}`}
          formElements={[MessageElement]}>
          <p>Are you sure you want to decommission this slave?</p>
          <pre>{slave.id}</pre>
          <p>Decommissioning a slave causes all tasks currently running on it to be rescheduled and executed elsewhere,
          as new tasks will no longer consider the slave with id <code>{slave.id}</code> a valid target for execution.
          This process may take time as replacement tasks must be considered healthy before old tasks are killed.</p>
        </ModalButton>
      );
    }
    return (
      <ModalButton
        buttonChildren={<Glyphicon glyph="remove" />}
        action="Remove Slave"
        onConfirm={(data) => this.props.removeSlave(slave, data.message)}
        tooltipText={`Remove ${slave.id}`}
        formElements={[MessageElement]}>
        <p>Are you sure you want to remove this slave?</p>
        <pre>{slave.id}</pre>
        {__in__(slave.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']) &&
        <p>
          Removing a decommissioned slave will cause that slave to become active again if the mesos-slave process is still running.
        </p>}
      </ModalButton>
    );
  }

  columnHeads(type) {
    const heads = ['ID', 'State', 'Since', 'Rack', 'Host', 'Uptime'];
    if (this.typeName[type]) {
      heads.push(this.typeName[type]);
    }
    heads.push('Message');
    heads.push(''); // Reactivate button and Decommission or Remove button
    return heads;
  }

  getRow(type, slave) {
    const now = +new Date();
    return (
      <tr key={slave.id}>
        <td>
          <a href={`${config.appRoot}/tasks/active/all/${slave.host}`} title={`All tasks running on host ${slave.host}`}>
            {slave.id}
          </a>
        </td>
        <td>{Utils.humanizeText(slave.currentState.state)}</td>
        <td>{Utils.absoluteTimestamp(slave.currentState.timestamp)}</td>
        <td>{slave.rackId}</td>
        <td>{slave.host}</td>
        <td>{Utils.duration(now - slave.firstSeenAt)}</td>
        {this.typeName[type] && <td>{this.showUser(slave) && slave.currentState.user}</td> }
        <td>{slave.currentState.message}</td>
        <td className="actions-column">
          {this.getMaybeReactivateButton(slave)}
          {this.getMaybeFreezeButton(slave)}
          {this.getDecommissionOrRemoveButton(slave)}
        </td>
      </tr>
    );
  }

  getSlaves(type, slaves) {
    return slaves.map(slave => this.getRow(type, slave));
  }

  getActiveSlaves() {
    return this.props.slaves.filter(({currentState}) => currentState.state === 'ACTIVE');
  }

  getFrozenSlaves() {
    return this.props.slaves.filter(({currentState}) => currentState.state === 'FROZEN');
  }

  getDecommissioningSlaves() {
    return this.props.slaves.filter(({currentState}) => __in__(currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']));
  }

  getInactiveSlaves() {
    return this.props.slaves.filter(({currentState}) => __in__(currentState.state, ['DEAD', 'MISSING_ON_STARTUP']));
  }

  getStates() {
    return [
      {
        stateName: 'Active',
        emptyMessage: 'No Active Slaves',
        headers: this.columnHeads('active'),
        hostsInState: this.getSlaves('active', this.getActiveSlaves())
      },
      {
        stateName: 'Frozen',
        emptyMessage: 'No Frozen Slaves',
        headers: this.columnHeads('frozen'),
        hostsInState: this.getSlaves('decommissioning', this.getFrozenSlaves())
      },
      {
        stateName: 'Decommissioning',
        emptyMessage: 'No Decommissioning Slaves',
        headers: this.columnHeads('decommissioning'),
        hostsInState: this.getSlaves('decommissioning', this.getDecommissioningSlaves())
      },
      {
        stateName: 'Inactive',
        emptyMessage: 'No Inactive Slaves',
        headers: this.columnHeads('inactive'),
        hostsInState: this.getSlaves('inactive', this.getInactiveSlaves())
      }
    ];
  }

  render() {
    return (
    <MachinesPage
      header = "Slaves"
      states = {this.getStates()}
      error = {this.props.error}
    />
    );
  }
}

Slaves.prototype.typeName = {
  'active': 'Activated By',
  'frozen': 'Frozen By',
  'decommissioning': 'Decommissioned By'
};

function getErrorFromState(state) {
  const { freezeSlave, decommissionSlave, removeSlave, reactivateSlave } = state.api;
  if (freezeSlave.error) {
    return `Error freezing slave: ${ state.api.freezeSlave.error.message }`;
  }
  if (decommissionSlave.error) {
    return `Error decommissioning slave: ${ state.api.decommissionSlave.error.message }`;
  }
  if (removeSlave.error) {
    return `Error removing slave: ${ state.api.removeSlave.error.message }`;
  }
  if (reactivateSlave.error) {
    return `Error reactivating slave: ${ state.api.reactivateSlave.error.message }`;
  }
  return null;
}

function mapStateToProps(state) {
  return {
    slaves: state.api.slaves.data,
    error: getErrorFromState(state)
  };
}

function mapDispatchToProps(dispatch) {
  function clear() {
    return Promise.all([
      dispatch(FreezeSlave.clear()),
      dispatch(DecommissionSlave.clear()),
      dispatch(RemoveSlave.clear()),
      dispatch(ReactivateSlave.clear())
    ]);
  }
  return {
    freezeSlave: (slave, message) => { clear().then(dispatch(FreezeSlave.trigger(slave.id, message)).then(dispatch(FetchSlaves.trigger()))); },
    decommissionSlave: (slave, message) => { clear().then(dispatch(DecommissionSlave.trigger(slave.id, message)).then(dispatch(FetchSlaves.trigger()))); },
    removeSlave: (slave, message) => { clear().then(dispatch(RemoveSlave.trigger(slave.id, message)).then(dispatch(FetchSlaves.trigger()))); },
    reactivateSlave: (slave, message) => { clear().then(dispatch(ReactivateSlave.trigger(slave.id, message)).then(dispatch(FetchSlaves.trigger()))); },
    clear
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Slaves);
