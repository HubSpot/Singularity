import React, {PropTypes} from 'react';
import MachinesPage from './MachinesPage';
import {Glyphicon} from 'react-bootstrap';
import FormModalButton from '../common/modal/FormModalButton';
import messageElement from './messageElement';
import Utils from '../../utils';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { Link } from 'react-router';
import { FetchSlaves, FreezeSlave, DecommissionSlave, RemoveSlave, ReactivateSlave } from '../../actions/api/slaves';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

const typeName = {
  'active': 'Activated By',
  'frozen': 'Frozen By',
  'decommissioning': 'Decommissioned By'
};

const Slaves = (props) => {
  const showUser = (slave) => Utils.isIn(slave.currentState.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']);

  const getMaybeReactivateButton = (slave) => (
    Utils.isIn(slave.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']) && (
      <FormModalButton
        name="Reactivate Slave"
        buttonChildren={<Glyphicon glyph="new-window" />}
        action="Reactivate Slave"
        onConfirm={(data) => props.reactivateSlave(slave, data.message)}
        tooltipText={`Reactivate ${slave.id}`}
        formElements={[messageElement]}>
        <p>Are you sure you want to cancel decommission and reactivate this slave??</p>
        <pre>{slave.id}</pre>
        <p>Reactivating a slave will cancel the decommission without erasing the slave's history and move it back to the active state.</p>
      </FormModalButton>
  ));

  const getMaybeFreezeButton = (slave) => (slave.currentState.state === 'ACTIVE' &&
    <FormModalButton
      name="Freeze Slave"
      buttonChildren={<Glyphicon glyph="stop" />}
      action="Freeze Slave"
      onConfirm={(data) => props.freezeSlave(slave, data.message)}
      tooltipText={`Freeze ${slave.id}`}
      formElements={[messageElement]}>
      <p>Are you sure you want to freeze this slave?</p>
      <pre>{slave.id}</pre>
      <p>Freezing a slave will prevent new tasks from being launched. Previously running tasks will be unaffected.</p>
    </FormModalButton>
  );

  const getMaybeDecommissionButton = (slave) => (Utils.isIn(slave.currentState.state, ['ACTIVE', 'FROZEN']) && (
    <FormModalButton
      name="Decommission Slave"
      buttonChildren={<Glyphicon glyph="trash" />}
      action="Decommission Slave"
      onConfirm={(data) => props.decommissionSlave(slave, data.message)}
      tooltipText={`Decommission ${slave.id}`}
      formElements={[messageElement]}>
      <p>Are you sure you want to decommission this slave?</p>
      <pre>{slave.id}</pre>
      <p>Decommissioning a slave causes all tasks currently running on it to be rescheduled and executed elsewhere,
      as new tasks will no longer consider the slave with id <code>{slave.id}</code> a valid target for execution.
      This process may take time as replacement tasks must be considered healthy before old tasks are killed.</p>
    </FormModalButton>
  ));

  const getMaybeRemoveButton = (slave) => (!Utils.isIn(slave.currentState.state, ['ACTIVE', 'FROZEN']) && (
    <FormModalButton
      name="Remove Slave"
      buttonChildren={<Glyphicon glyph="remove" />}
      action="Remove Slave"
      onConfirm={(data) => props.removeSlave(slave, data.message)}
      tooltipText={`Remove ${slave.id}`}
      formElements={[messageElement]}>
      <p>Are you sure you want to remove this slave?</p>
      <pre>{slave.id}</pre>
      {Utils.isIn(slave.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']) &&
      <p>
        Removing a decommissioned slave will cause that slave to become active again if the mesos-slave process is still running.
      </p>}
    </FormModalButton>
  ));

  const getColumns = (type) => {
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

    if (typeName[type]) {
      columns.push(
        <Column
          label={typeName[type]}
          id="typename"
          key="typename"
          sortable={true}
          sortData={(cellData, slave) => slave.currentState.user || ''}
          cellData={(slave) => showUser(slave) && slave.currentState.user}
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
            {getMaybeReactivateButton(slave)}
            {getMaybeFreezeButton(slave)}
            {getMaybeDecommissionButton(slave)}
            {getMaybeRemoveButton(slave)}
            <JSONButton object={slave} showOverlay={true}>
              {'{ }'}
            </JSONButton>
          </span>
        )}
      />
    );

    return columns;
  };

  const activeSlaves = props.slaves.filter(({currentState}) => currentState.state === 'ACTIVE');

  const frozenSlaves = props.slaves.filter(({currentState}) => currentState.state === 'FROZEN');

  const decommissioningSlaves = props.slaves.filter(({currentState}) => Utils.isIn(currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']));

  const inactiveSlaves = props.slaves.filter(({currentState}) => Utils.isIn(currentState.state, ['DEAD', 'MISSING_ON_STARTUP']));

  const states = [
    {
      stateName: 'Active',
      emptyMessage: 'No Active Slaves',
      hostsInState: activeSlaves,
      columns: getColumns('active')
    },
    {
      stateName: 'Frozen',
      emptyMessage: 'No Frozen Slaves',
      hostsInState: frozenSlaves,
      columns: getColumns('decommissioning')
    },
    {
      stateName: 'Decommissioning',
      emptyMessage: 'No Decommissioning Slaves',
      hostsInState: decommissioningSlaves,
      columns: getColumns('decommissioning')
    },
    {
      stateName: 'Inactive',
      emptyMessage: 'No Inactive Slaves',
      hostsInState: inactiveSlaves,
      columns: getColumns('inactive')
    }
  ];

  return (
    <MachinesPage
      header = "Slaves"
      states = {states}
      error = {props.error}
    />
  );
};

Slaves.propTypes = {
  freezeSlave: PropTypes.func.isRequired,
  decommissionSlave: PropTypes.func.isRequired,
  removeSlave: PropTypes.func.isRequired,
  reactivateSlave: PropTypes.func.isRequired,
  clear: PropTypes.func.isRequired,
  error: PropTypes.string,
  slaves: PropTypes.arrayOf(PropTypes.shape({
    state: PropTypes.string
  }))
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
    freezeSlave: (slave, message) => { clear().then(() => dispatch(FreezeSlave.trigger(slave.id, message)).then(() => dispatch(FetchSlaves.trigger()))); },
    decommissionSlave: (slave, message) => { clear().then(() => dispatch(DecommissionSlave.trigger(slave.id, message)).then(() => dispatch(FetchSlaves.trigger()))); },
    removeSlave: (slave, message) => { clear().then(() => dispatch(RemoveSlave.trigger(slave.id, message)).then(() => dispatch(FetchSlaves.trigger()))); },
    reactivateSlave: (slave, message) => { clear().then(() => dispatch(ReactivateSlave.trigger(slave.id, message)).then(() => dispatch(FetchSlaves.trigger()))); },
    clear
  };
}

function initialize(props) {
  return Promise.all([
    props.clear(),
    props.fetchSlaves()
  ]);
}

function refresh(props) {
  return props.fetchSlaves();
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(Slaves, 'Slaves', refresh, true, true, initialize));
