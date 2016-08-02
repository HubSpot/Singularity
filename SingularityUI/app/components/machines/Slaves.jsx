import React, {PropTypes} from 'react';
import MachinesPage from './MachinesPage';
import {Glyphicon} from 'react-bootstrap';
import ModalButton from './ModalButton';
import messageElement from './messageElement';
import Utils from '../../utils';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { Link } from 'react-router';
import { FetchSlaves, FreezeSlave, DecommissionSlave, RemoveSlave, ReactivateSlave } from '../../actions/api/slaves';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

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
    return Utils.isIn(slave.currentState.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']);
  }

  getMaybeReactivateButton(slave) {
    return (Utils.isIn(slave.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']) &&
      <ModalButton
        buttonChildren={<Glyphicon glyph="new-window" />}
        action="Reactivate Slave"
        onConfirm={(data) => this.props.reactivateSlave(slave, data.message)}
        tooltipText={`Reactivate ${slave.id}`}
        formElements={[messageElement]}>
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
        formElements={[messageElement]}>
        <p>Are you sure you want to freeze this slave?</p>
        <pre>{slave.id}</pre>
        <p>Freezing a slave will prevent new tasks from being launched. Previously running tasks will be unaffected.</p>
      </ModalButton>
    );
  }

  getDecommissionOrRemoveButton(slave) {
    if (Utils.isIn(slave.currentState.state, ['ACTIVE', 'FROZEN'])) {
      return (
        <ModalButton
          buttonChildren={<Glyphicon glyph="trash" />}
          action="Decommission Slave"
          onConfirm={(data) => this.props.decommissionSlave(slave, data.message)}
          tooltipText={`Decommission ${slave.id}`}
          formElements={[messageElement]}>
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
        formElements={[messageElement]}>
        <p>Are you sure you want to remove this slave?</p>
        <pre>{slave.id}</pre>
        {Utils.isIn(slave.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']) &&
        <p>
          Removing a decommissioned slave will cause that slave to become active again if the mesos-slave process is still running.
        </p>}
      </ModalButton>
    );
  }

  getColumns(type) {
    const columns = [
      <Column
        label="ID"
        id="id"
        key="id"
        sortable={true}
        sortData={(cellData, slave) => slave.id}
        cellData={(slave) => (
          <Link to={`tasks/active/all/${slave.host}`} title={`All tasks running on host ${slave.host}`}>
            {slave.id}
          </Link>
        )}
      />,
      <Column
        label="State"
        id="state"
        key="state"
        sortable={true}
        sortData={(cellData, slave) => slave.currentState.state}
        cellData={(slave) => Utils.humanizeText(slave.currentState.state)}
      />,
      <Column
        label="Since"
        id="timestamp"
        key="timestamp"
        sortable={true}
        sortData={(cellData, slave) => slave.currentState.timestamp}
        cellData={(slave) => Utils.absoluteTimestamp(slave.currentState.timestamp)}
      />,
      <Column
        label="Rack"
        id="rack"
        key="rack"
        sortable={true}
        sortData={(cellData, slave) => slave.rackId}
        cellData={(slave) => slave.rackId}
      />,
      <Column
        label="Host"
        id="host"
        key="host"
        sortable={true}
        sortData={(cellData, slave) => slave.host}
        cellData={(slave) => slave.host}
      />,
      <Column
        label="Uptime"
        id="uptime"
        key="uptime"
        sortable={true}
        sortData={(cellData, slave) => slave.firstSeenAt}
        cellData={(slave) => Utils.duration(Date.now() - slave.firstSeenAt)}
      />
    ];

    if (this.typeName[type]) {
      columns.push(
        <Column
          label={this.typeName[type]}
          id="typename"
          key="typename"
          sortable={true}
          sortData={(cellData, slave) => slave.currentState.user || ''}
          cellData={(slave) => this.showUser(slave) && slave.currentState.user}
        />
      );
    }
    columns.push(
      <Column
        label="Message"
        id="message"
        key="message"
        cellData={(slave) => slave.currentState.message}
      />,
      <Column
        id="actions-column"
        key="actions-column"
        className="actions-column"
        cellData={(slave) => (
          <span>
            {this.getMaybeReactivateButton(slave)}
            {this.getMaybeFreezeButton(slave)}
            {this.getDecommissionOrRemoveButton(slave)}
            <JSONButton object={slave}>
              {'{ }'}
            </JSONButton>
          </span>
        )}
      />
    );

    return columns;
  }

  getActiveSlaves() {
    return this.props.slaves.filter(({currentState}) => currentState.state === 'ACTIVE');
  }

  getFrozenSlaves() {
    return this.props.slaves.filter(({currentState}) => currentState.state === 'FROZEN');
  }

  getDecommissioningSlaves() {
    return this.props.slaves.filter(({currentState}) => Utils.isIn(currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']));
  }

  getInactiveSlaves() {
    return this.props.slaves.filter(({currentState}) => Utils.isIn(currentState.state, ['DEAD', 'MISSING_ON_STARTUP']));
  }

  getStates() {
    return [
      {
        stateName: 'Active',
        emptyMessage: 'No Active Slaves',
        hostsInState: this.getActiveSlaves(),
        columns: this.getColumns('active')
      },
      {
        stateName: 'Frozen',
        emptyMessage: 'No Frozen Slaves',
        hostsInState: this.getFrozenSlaves(),
        columns: this.getColumns('decommissioning')
      },
      {
        stateName: 'Decommissioning',
        emptyMessage: 'No Decommissioning Slaves',
        hostsInState: this.getDecommissioningSlaves(),
        columns: this.getColumns('decommissioning')
      },
      {
        stateName: 'Inactive',
        emptyMessage: 'No Inactive Slaves',
        hostsInState: this.getInactiveSlaves(),
        columns: this.getColumns('inactive')
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
    fetchSlaves: () => dispatch(FetchSlaves.trigger()),
    freezeSlave: (slave, message) => { clear().then(dispatch(FreezeSlave.trigger(slave.id, message)).then(dispatch(FetchSlaves.trigger()))); },
    decommissionSlave: (slave, message) => { clear().then(dispatch(DecommissionSlave.trigger(slave.id, message)).then(dispatch(FetchSlaves.trigger()))); },
    removeSlave: (slave, message) => { clear().then(dispatch(RemoveSlave.trigger(slave.id, message)).then(dispatch(FetchSlaves.trigger()))); },
    reactivateSlave: (slave, message) => { clear().then(dispatch(ReactivateSlave.trigger(slave.id, message)).then(dispatch(FetchSlaves.trigger()))); },
    clear
  };
}

function refresh(props) {
  return props.fetchSlaves();
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(Slaves, 'Slaves', refresh));
