import React, {PropTypes} from 'react';
import MachinesPage from './MachinesPage';
import {Glyphicon} from 'react-bootstrap';
import FormModalButton from '../common/modal/FormModalButton';
import FormModal from '../common/modal/FormModal';
import Utils from '../../utils';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { Link } from 'react-router';
import { FetchSlaves, FreezeSlave, DecommissionSlave, RemoveSlave, ReactivateSlave, FetchExpiringSlaveStates, RemoveExpiringSlaveState } from '../../actions/api/slaves';
import { DeactivateHost, ReactivateHost, FetchInactiveHosts } from '../../actions/api/inactive';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';
import { refresh, initialize } from '../../actions/ui/slaves'
import CustomizeSlavesTableButton from './CustomizeSlavesTableButton';

const typeName = {
  'active': 'Activated By',
  'frozen': 'Frozen By',
  'decommissioning': 'Decommissioned By',
  'decommissioned': 'Decommissioned By'
};

const Slaves = (props) => {
  const actionElements = (slave, buttonType) => {
    const elements = [];
    elements.push({
      name: 'message',
      type: FormModal.INPUT_TYPES.STRING,
      label: 'Message (optional)'
    });
    if (buttonType != 'REMOVE') {
      elements.push({
        name: 'durationMillis',
        type: FormModal.INPUT_TYPES.DURATION,
        label: 'Expiration (optional)',
        help: (
          <div>
            <p>If an expiration duration is specified, the slave will revert to the state specified below after time has elapsed.</p>
          </div>
        )
      });
      elements.push({
        name: 'revertToState',
        type: FormModal.INPUT_TYPES.SELECT,
        dependsOn: 'durationMillis',
        defaultValue: slave.currentState.state,
        label: 'Revert To',
        options: _.map(Utils.MACHINE_STATES_FOR_REVERT, (machineState) => ({
          label: machineState,
          value: machineState
        }))
      });
    }
    if (buttonType == 'DECOMMISSION') {
      elements.push({
        name: 'killTasksOnDecommissionTimeout',
        type: FormModal.INPUT_TYPES.BOOLEAN,
        dependsOn: 'durationMillis',
        label: 'Kill remaining tasks on decommission timeout',
        defaultValue: false
      });
    }

    return elements;
  };

  const showUser = (slave) => Utils.isIn(slave.currentState.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']);

  const resources = _.uniq(_.flatten(_.map(props.slaves, (slave) => Object.keys(Utils.maybe(slave, ['resources'], [])))));

  const customAttrs = _.uniq(_.flatten(_.map(props.slaves, (slave) => Object.keys(Utils.maybe(slave, ['attributes'], [])))));

  const expiringSlaveState = (slave) => (
    _.find(props.expiringSlaveStates, (expiring) => expiring.machineId == slave.id)
  );

  const getMaybeDeactivateHostButton = (slave) => (
    ! Utils.isIn(slave.host, props.inactiveHosts) && (
      <FormModalButton
        name="Mark Inactive"
        buttonChildren={<Glyphicon glyph="remove-circle" />}
        action="Mark Host Inactive"
        onConfirm={() => props.deactivateHost(slave.host)}
        tooltipText={`Flag host '${slave.host}' as inactive`}
        formElements={[]}
      >
        <p>Are you sure you want to mark the host {slave.host} as inactive?</p>
        <p>
          This will automatically decommission any slave that joins with this hostname.
        </p>
      </FormModalButton>
    )
  );

  const getMaybeReactivateHostButton = (slave) => (
    Utils.isIn(slave.host, props.inactiveHosts) && (
      <FormModalButton
        name="Reactivate Host"
        buttonChildren={<Glyphicon glyph="ok-circle" />}
        action="Mark Host Active"
        onConfirm={() => props.reactivateHost(slave.host)}
        tooltipText={`Mark host '${slave.host}' as active`}
        formElements={[]}
      >
        <p>Are you sure you want to reactivate host {slave.host}?</p>
        <p>
          New slaves from this host will no longer automatically be marked as
          decommissioned.
        </p>
      </FormModalButton>
    )
  );

  const getMaybeReactivateButton = (slave) => (
    Utils.isIn(slave.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']) && (
      <FormModalButton
        name="Reactivate Slave"
        buttonChildren={<Glyphicon glyph="new-window" />}
        action="Reactivate Slave"
        onConfirm={(data) => props.reactivateSlave(slave, data)}
        tooltipText={`Reactivate ${slave.id}`}
        formElements={actionElements(slave, 'REACTIVATE')}>
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
      onConfirm={(data) => props.freezeSlave(slave, data)}
      tooltipText={`Freeze ${slave.id}`}
      formElements={actionElements(slave, 'FREEZE')}>
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
      onConfirm={(data) => props.decommissionSlave(slave, data)}
      tooltipText={`Decommission ${slave.id}`}
      formElements={actionElements(slave, 'DECOMMISSION')}>
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
      onConfirm={(data) => props.removeSlave(slave, data)}
      tooltipText={`Remove ${slave.id}`}
      formElements={actionElements(slave, 'REMOVE')}>
      <p>Are you sure you want to remove this slave?</p>
      <pre>{slave.id}</pre>
      {Utils.isIn(slave.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']) &&
      <p>
        Removing a decommissioned slave will cause that slave to become active again if the mesos-slave process is still running.
      </p>}
    </FormModalButton>
  ));

  const getMaybeRemoveExpiring = (slave) => ( expiringSlaveState(slave) && (
    <FormModalButton
      name="Cancel Expiring ACtion"
      buttonChildren={<Glyphicon glyph="remove-circle" />}
      action={`Make ${slave.currentState.state} Permanent`}
      onConfirm={(data) => props.removeExpiringState(slave.id)}
      tooltipText={`Cancel revert to ${expiringSlaveState(slave).revertToState}`}
      formElements={[]}>
      <p>Are you sure you want to remove the expiring action for this slave? This will make the curretn state permanent.</p>
      <pre>{slave.id}</pre>
    </FormModalButton>
  ));

  const idColumn = () => (
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
    />
  );

  const stateColumn = () => (
    <Column
      label="State"
      id="state"
      key="state"
      sortable={true}
      sortData={(cellData, slave) => slave.currentState.state}
      cellData={(slave) => Utils.humanizeText(slave.currentState.state)}
    />
  );

  const sinceColumn = () => (
    <Column
      label="Since"
      id="timestamp"
      key="timestamp"
      sortable={true}
      sortData={(cellData, slave) => slave.currentState.timestamp}
      cellData={(slave) => Utils.absoluteTimestamp(slave.currentState.timestamp)}
    />
  );

  const rackColumn = () => (
    <Column
      label="Rack"
      id="rack"
      key="rack"
      sortable={true}
      sortData={(cellData, slave) => slave.rackId}
      cellData={(slave) => slave.rackId}
    />
  );

  const hostColumn = () => (
    <Column
      label="Host"
      id="host"
      key="host"
      sortable={true}
      sortData={(cellData, slave) => slave.host}
      cellData={(slave) => slave.host}
    />
  );

  const uptimeColumn = () => (
    <Column
      label="Uptime"
      id="uptime"
      key="uptime"
      sortable={true}
      sortData={(cellData, slave) => slave.firstSeenAt}
      cellData={(slave) => Utils.duration(Date.now() - slave.firstSeenAt)}
    />
  );

  const messageColumn = () => (
    <Column
      label="Message"
      id="message"
      key="message"
      cellData={(slave) => slave.currentState.message}
    />
  );

  const expiringColumn = () => (
    <Column
      label="Expiring"
      id="expiring"
      key="expiring"
      cellData={(slave) => {
        const expiring = expiringSlaveState(slave);
        if (expiring) {
          return `Transitions to ${expiring.revertToState} in ${Utils.duration(Date.now() - (expiring.startMillis + expiring.expiringAPIRequestObject.durationMillis))}`
        }
      }}
    />
  );

  const getColumns = (type) => {
    const columns = [];
    if (props.columnSettings['id']) {
      columns.push(idColumn());
    }
    if (props.columnSettings['state']) {
      columns.push(stateColumn());
    }
    if (props.columnSettings['since']) {
      columns.push(sinceColumn());
    }
    if (props.columnSettings['rack']) {
      columns.push(rackColumn());
    }
    if (props.columnSettings['host']) {
      columns.push(hostColumn());
    }
    if (props.columnSettings['uptime']) {
      columns.push(uptimeColumn());
    }

    if (typeName[type] && props.columnSettings['actionUser']) {
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
    if (props.columnSettings['message']) {
      columns.push(messageColumn());
    }
    if (!_.isEmpty(props.expiringSlaveStates) && props.columnSettings['expiring']) {
      columns.push(expiringColumn());
    }

    _.each(resources, (resource) => {
        if (props.columnSettings[resource]) {
          columns.push(
            <Column
              label={resource}
              id={resource}
              key={resource}
              sortable={true}
              cellData={(slave) => Utils.maybe(slave, ['resources', resource], 0)}
            />
          );
        }
      }
    );

    _.each(customAttrs, (attr) => {
        if (props.columnSettings[attr]) {
          columns.push(
            <Column
              label={attr}
              id={attr}
              key={attr}
              sortable={true}
              cellData={(slave) => Utils.maybe(slave, ['attributes', attr], '')}
            />
          );
        }
      }
    );

    columns.push(
      <Column
        id="actions-column"
        key="actions-column"
        className="actions-column"
        cellData={(slave) => (
          <span>
            {getMaybeDeactivateHostButton(slave)}
            {getMaybeReactivateHostButton(slave)}
            {getMaybeRemoveExpiring(slave)}
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

  const decommissioningSlaves = props.slaves.filter(({currentState}) => Utils.isIn(currentState.state, ['DECOMMISSIONING', 'STARTING_DECOMMISSION']));

  const decommissionedSlaves = props.slaves.filter(({currentState}) => currentState.state === 'DECOMMISSIONED');

  const inactiveSlaves = props.slaves.filter(({currentState}) => Utils.isIn(currentState.state, ['DEAD', 'MISSING_ON_STARTUP']));

  const inactiveHostsPanel = (inactiveHosts) => (
    inactiveHosts && inactiveHosts.length > 0 && (
      <div className="row">
        <h3>Inactive Hosts</h3>
        <p>These hosts are marked as inactive: </p>
        <ul className="list-group">
          {inactiveHosts.map((host) => (
            <li className="list-group-item" key={host}>
              {host}
              <span className="pull-right">
                {getMaybeReactivateHostButton({host})}
              </span>
            </li>
          ))}
        </ul>
      </div>
    )
  );

  const states = [
    {
      stateName: 'Active',
      emptyMessage: 'No Active Slaves',
      hostsInState: activeSlaves,
      columns: getColumns('active'),
      paginated: props.paginated
    },
    {
      stateName: 'Frozen',
      emptyMessage: 'No Frozen Slaves',
      hostsInState: frozenSlaves,
      columns: getColumns('decommissioning'),
      paginated: props.paginated
    },
    {
      stateName: 'Decommissioning',
      emptyMessage: 'No Decommissioning Slaves',
      hostsInState: decommissioningSlaves,
      columns: getColumns('decommissioning'),
      paginated: props.paginated
    },
    {
      stateName: 'Decommissioned',
      emptyMessage: 'No Decommissioned Slaves',
      hostsInState: decommissionedSlaves,
      columns: getColumns('decommissioned'),
      paginated: props.paginated
    },
    {
      stateName: 'Inactive',
      emptyMessage: 'No Inactive Slaves',
      hostsInState: inactiveSlaves,
      columns: getColumns('inactive'),
      paginated: props.paginated
    }
  ];

  return (
    <div>
      <CustomizeSlavesTableButton
        columns={props.columnSettings}
        paginated={props.paginated}
        availableAttributes={customAttrs}
        availableResources={resources}
      >
        <button
          className="btn btn-primary pull-right"
          alt="Customize Columns"
          title="Customize">
          Customize
        </button>
      </CustomizeSlavesTableButton>
      <MachinesPage
        header = "Slaves"
        states = {states}
        error = {props.error}
      />
    {inactiveHostsPanel(props.inactiveHosts)}
    </div>
  );
};

Slaves.propTypes = {
  freezeSlave: PropTypes.func.isRequired,
  decommissionSlave: PropTypes.func.isRequired,
  removeSlave: PropTypes.func.isRequired,
  reactivateSlave: PropTypes.func.isRequired,
  fetchExpiringSlaveStates: PropTypes.func.isRequired,
  removeExpiringState: PropTypes.func.isRequired,
  clear: PropTypes.func.isRequired,
  error: PropTypes.string,
  expiringSlaveStates: PropTypes.array.isRequired,
  slaves: PropTypes.arrayOf(PropTypes.shape({
    state: PropTypes.string
  })),
  inactiveHosts: PropTypes.arrayOf(PropTypes.string),
  columnSettings: PropTypes.object.isRequired,
  paginated: PropTypes.bool.isRequired
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
    inactiveHosts: state.api.inactiveHosts.data,
    slaves: state.api.slaves.data,
    error: getErrorFromState(state),
    columnSettings: state.ui.slaves.columns,
    paginated: state.ui.slaves.paginated,
    expiringSlaveStates: state.api.expiringSlaveStates.data
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
  function fetchSlavesAndExpiring() {
    return Promise.all([
      dispatch(FetchSlaves.trigger()),
      dispatch(FetchExpiringSlaveStates.trigger())
    ]);
  }
  return {
    fetchSlaves: () => dispatch(FetchSlaves.trigger()),
    freezeSlave: (slave, message) => { clear().then(() => dispatch(FreezeSlave.trigger(slave.id, message)).then(() => fetchSlavesAndExpiring())); },
    decommissionSlave: (slave, message) => { clear().then(() => dispatch(DecommissionSlave.trigger(slave.id, message)).then(() => fetchSlavesAndExpiring())); },
    removeSlave: (slave, message) => { clear().then(() => dispatch(RemoveSlave.trigger(slave.id, message)).then(() => fetchSlavesAndExpiring())); },
    deactivateHost: (host) =>
      clear()
        .then(() => dispatch(DeactivateHost.trigger(host)))
        .then(() => Promise.all([
          fetchSlavesAndExpiring(),
          dispatch(FetchInactiveHosts.trigger()),
        ])),
    reactivateHost: (host) =>
      clear()
        .then(() => dispatch(ReactivateHost.trigger(host)))
        .then(() => Promise.all([
          fetchSlavesAndExpiring(),
          dispatch(FetchInactiveHosts.trigger()),
        ])),
    fetchInactiveHosts: () => dispatch(FetchInactiveHosts.trigger()),
    reactivateSlave: (slave, message) => { clear().then(() => dispatch(ReactivateSlave.trigger(slave.id, message)).then(() => fetchSlavesAndExpiring())); },
    fetchExpiringSlaveStates: () => dispatch(FetchExpiringSlaveStates.trigger()),
    removeExpiringState: (slaveId) => { clear().then(() => dispatch(RemoveExpiringSlaveState.trigger(slaveId)).then(() => fetchSlavesAndExpiring())); },
    clear
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(Slaves, refresh, true, true, initialize));
