import React, { PropTypes } from 'react';
import MachinesPage from './MachinesPage';
import {Glyphicon} from 'react-bootstrap';
import FormModalButton from '../common/modal/FormModalButton';
import FormModal from '../common/modal/FormModal';
import Utils from '../../utils';
import { connect } from 'react-redux';
import { DecommissionRack, RemoveRack, ReactivateRack, FreezeRack, FetchRacks } from '../../actions/api/racks.es6';
import rootComponent from '../../rootComponent';
import { Link } from 'react-router';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';
import { refresh, initialize } from '../../actions/ui/racks';

const typeName = {
  'active': 'Activated By',
  'frozen': 'Frozen By',
  'decommissioning': 'Decommissioned By'
};

const Racks = (props) => {
  const messageElement = {
    name: 'message',
    type: FormModal.INPUT_TYPES.STRING,
    label: 'Message (optional)'
  }

  const showUser = (rack) => Utils.isIn(rack.currentState.state, ['ACTIVE', 'FROZEN', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']);

  const getMaybeReactivateButton = (rack) => (Utils.isIn(rack.currentState.state, ['FROZEN', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']) &&
    <FormModalButton
      name="Reactivate Rack"
      buttonChildren={<Glyphicon glyph="new-window" />}
      action="Reactivate Rack"
      onConfirm={(data) => props.reactivateRack(rack, data.message)}
      tooltipText={`Reactivate ${rack.id}`}
      formElements={[messageElement]}>
      <p>Are you sure you want to cancel decommission/freeze and reactivate this rack??</p>
      <pre>{rack.id}</pre>
      <p>Reactivating a rack will cancel the decommission/freeze without erasing the rack's history and move it back to the active state.</p>
    </FormModalButton>
  );

  const getMaybeDecommissionButton = (rack) => (Utils.isIn(rack.currentState.state, ['ACTIVE', 'FROZEN']) && (
    <FormModalButton
      name="Decommission Rack"
      buttonChildren={<Glyphicon glyph="trash" />}
      action="Decommission Rack"
      onConfirm={(data) => props.decommissionRack(rack, data.message)}
      tooltipText={`Decommission ${rack.id}`}
      formElements={[messageElement]}>
      <p>Are you sure you want to decommission this rack?</p>
      <pre>{rack.id}</pre>
      <p>
        Decommissioning a rack causes all tasks currently running on it to be rescheduled and executed elsewhere,
        as new tasks will no longer consider the rack with id <code>{rack.id}</code> a valid target for execution.
        This process may take time as replacement tasks must be considered healthy before old tasks are killed.
      </p>
    </FormModalButton>
  ));

  const getMaybeFreezeButton = (rack) => (rack.currentState.state === 'ACTIVE' && (
    <FormModalButton
      name="Freeze Rack"
      buttonChildren={<Glyphicon glyph="pause" />}
      action="Freeze Rack"
      onConfirm={(data) => props.freezeRack(rack, data.message)}
      tooltipText={`Freeze ${rack.id}`}
      formElements={[messageElement]}>
      <p>Are you sure you want to freeze this rack?</p>
      <pre>{rack.id}</pre>
      <p>
        Freezing a rack prevents new tasks from being scheduled on the rack, but tasks currently running on it will not be rescheduled.
        New tasks will no longer consider the rack with id <code>{rack.id}</code> a valid target for execution.
        This process should be near-instant, as no tasks need to be replaced/killed.
      </p>
    </FormModalButton>
  ));

  const getMaybeRemoveButton = (rack) => (rack.currentState.state !== 'ACTIVE' && (
    <FormModalButton
      name="Remove Rack"
      buttonChildren={<Glyphicon glyph="remove" />}
      action="Remove Rack"
      onConfirm={(data) => props.removeRack(rack, data.message)}
      tooltipText={`Remove ${rack.id}`}
      formElements={[messageElement]}>
      <p>Are you sure you want to remove this rack??</p>
      <pre>{rack.id}</pre>
      <p>Removing a decommissioned rack will cause that rack to become active again if the mesos-rack process is still running.</p>
    </FormModalButton>
  ));

  const getColumns = (type) => {
    const columns = ([
      <Column
        label="ID"
        id="id"
        key="id"
        sortable={true}
        sortData={(cellData, rack) => rack.id}
        cellData={(rack) => (
          <Link to={`tasks/active/all/${rack.id}`} title={`All tasks running on rack ${rack.id}`}>
            {rack.id}
          </Link>
        )}
      />,
      <Column
        label="Current State"
        id="state"
        key="state"
        sortable={true}
        sortData={(cellData, rack) => rack.currentState.state}
        cellData={(rack) => Utils.humanizeText(rack.currentState.state)}
      />,
      <Column
        label="Uptime"
        id="uptime"
        key="uptime"
        sortable={true}
        sortData={(cellData, rack) => rack.firstSeenAt}
        cellData={(rack) => Utils.duration(Date.now() - rack.firstSeenAt)}
      />
    ]);
    if (typeName[type]) {
      columns.push(
        <Column
          label={typeName[type]}
          id="typename"
          key="typename"
          sortable={true}
          sortData={(cellData, rack) => rack.currentState.user || ''}
          cellData={(rack) => showUser(rack) && rack.currentState.user}
        />
      );
    }
    columns.push(
      <Column
        label="Message"
        id="message"
        key="message"
        cellData={(rack) => rack.currentState.message}
      />,
      <Column
        id="actions-column"
        key="actions-column"
        className="actions-column"
        cellData={(rack) => (
          <span>
            {getMaybeReactivateButton(rack)}
            {getMaybeDecommissionButton(rack)}
            {getMaybeFreezeButton(rack)}
            {getMaybeRemoveButton(rack)}
            <JSONButton object={rack} showOverlay={true}>
              {'{ }'}
            </JSONButton>
          </span>
        )}
      />
    );
    return columns;
  };

  const activeRacks = props.racks.filter(({currentState}) => Utils.isIn(currentState.state, ['ACTIVE']));

  const decommissioningRacks = props.racks.filter(({currentState}) => Utils.isIn(currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']));
  const frozenRacks = props.racks.filter(({currentState}) => Utils.isIn(currentState.state, ['FROZEN']));
  const inactiveRacks = props.racks.filter(({currentState}) => Utils.isIn(currentState.state, ['DEAD', 'MISSING_ON_STARTUP']));

  const states = [
    {
      stateName: 'Active',
      emptyMessage: 'No Active Racks',
      columns: getColumns('active'),
      hostsInState: activeRacks
    },
    {
      stateName: 'Decommissioning',
      emptyMessage: 'No Decommissioning Racks',
      columns: getColumns('decommissioning'),
      hostsInState: decommissioningRacks
    },
    {
      stateName: 'Frozen',
      emptyMessage: 'No Frozen Racks',
      columns: getColumns('frozen'),
      hostsInState: frozenRacks
    },
    {
      stateName: 'Inactive',
      emptyMessage: 'No Inactive Racks',
      columns: getColumns('inactive'),
      hostsInState: inactiveRacks
    }
  ];

  return (
    <MachinesPage
      header="Racks"
      states={states}
      error={props.error}
    />
  );
};

Racks.propTypes = {
  racks: PropTypes.arrayOf(PropTypes.shape({
    state: PropTypes.string
  })),
  removeRack: PropTypes.func.isRequired,
  freezeRack: PropTypes.func.isRequired,
  decommissionRack: PropTypes.func.isRequired,
  reactivateRack: PropTypes.func.isRequired,
  clear: PropTypes.func.isRequired,
  error: PropTypes.string
};

function getErrorFromState(state) {
  const { decommissionRack, removeRack, reactivateRack, freezeRack } = state.api;
  if (decommissionRack.error) {
    return `Error decommissioning rack: ${ state.api.decommissionRack.error.message }`;
  }
  if (removeRack.error) {
    return `Error removing rack: ${ state.api.removeRack.error.message }`;
  }
  if (reactivateRack.error) {
    return `Error reactivating rack: ${ state.api.reactivateRack.error.message }`;
  }
  if (freezeRack.error) {
    return `Error freezing rack: ${state.api.freezeRack.error.message}`;
  }
  return null;
}

function mapStateToProps(state) {
  return {
    racks: state.api.racks.data,
    error: getErrorFromState(state)
  };
}

function mapDispatchToProps(dispatch) {
  function clear() {
    return Promise.all([
      dispatch(DecommissionRack.clear()),
      dispatch(FreezeRack.clear()),
      dispatch(RemoveRack.clear()),
      dispatch(ReactivateRack.clear())
    ]);
  }
  return {
    decommissionRack: (rack, message) => { clear().then(() => dispatch(DecommissionRack.trigger(rack.id, message))).then(() => dispatch(FetchRacks.trigger())); },
    freezeRack: (rack, message) => { clear().then(() => dispatch(FreezeRack.trigger(rack.id, message))).then(() => dispatch(FetchRacks.trigger())); },
    removeRack: (rack, message) => { clear().then(() => dispatch(RemoveRack.trigger(rack.id, message))).then(() => dispatch(FetchRacks.trigger())); },
    reactivateRack: (rack, message) => { clear().then(() => dispatch(ReactivateRack.trigger(rack.id, message))).then(() => dispatch(FetchRacks.trigger())); },
    fetchRacks: () => dispatch(FetchRacks.trigger()),
    clear
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(Racks, refresh, true, true, initialize));
