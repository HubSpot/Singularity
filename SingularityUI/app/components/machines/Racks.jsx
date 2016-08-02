import React, {PropTypes} from 'react';
import MachinesPage from './MachinesPage';
import {Glyphicon} from 'react-bootstrap';
import ModalButton from './ModalButton';
import messageElement from './messageElement';
import Utils from '../../utils';
import { connect } from 'react-redux';
import { DecommissionRack, RemoveRack, ReactivateRack, FetchRacks } from '../../actions/api/racks';
import rootComponent from '../../rootComponent';
import { Link } from 'react-router';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

const Racks = React.createClass({

  propTypes: {
    racks: PropTypes.arrayOf(PropTypes.shape({
      state: PropTypes.string
    })),
    decommissionRack: PropTypes.func.isRequired,
    removeRack: PropTypes.func.isRequired,
    reactivateRack: PropTypes.func.isRequired,
    clear: PropTypes.func.isRequired,
    error: PropTypes.string
  },

  componentWillUnmount() {
    this.props.clear();
  },

  typeName: {
    'active': 'Activated By',
    'frozen': 'Frozen By',
    'decommissioning': 'Decommissioned By'
  },

  showUser(rack) {
    return Utils.isIn(rack.currentState.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']);
  },

  getMaybeReactivateButton(rack) {
    return (Utils.isIn(rack.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']) &&
      <ModalButton
        buttonChildren={<Glyphicon glyph="new-window" />}
        action="Reactivate Rack"
        onConfirm={(data) => this.props.reactivateRack(rack, data.message)}
        tooltipText={`Reactivate ${rack.id}`}
        formElements={[messageElement]}>
        <p>Are you sure you want to cancel decommission and reactivate this rack??</p>
        <pre>{rack.id}</pre>
        <p>Reactivating a rack will cancel the decommission without erasing the rack's history and move it back to the active state.</p>
      </ModalButton>
    );
  },

  getDecommissionOrRemoveButton(rack) {
    if (rack.currentState.state === 'ACTIVE') {
      return (
        <ModalButton
          buttonChildren={<Glyphicon glyph="trash" />}
          action="Decommission Rack"
          onConfirm={(data) => this.props.decommissionRack(rack, data.message)}
          tooltipText={`Decommission ${rack.id}`}
          formElements={[messageElement]}>
          <p>Are you sure you want to decommission this rack?</p>
          <pre>{rack.id}</pre>
          <p>
            Decommissioning a rack causes all tasks currently running on it to be rescheduled and executed elsewhere,
            as new tasks will no longer consider the rack with id <code>{rack.id}</code> a valid target for execution.
            This process may take time as replacement tasks must be considered healthy before old tasks are killed.
          </p>
        </ModalButton>
      );
    }
    return (
      <ModalButton
        buttonChildren={<Glyphicon glyph="remove" />}
        action="Remove Rack"
        onConfirm={(data) => this.props.removeRack(rack, data.message)}
        tooltipText={`Remove ${rack.id}`}
        formElements={[messageElement]}>
        <p>Are you sure you want to remove this rack??</p>
        <pre>{rack.id}</pre>
        <p>Removing a decommissioned rack will cause that rack to become active again if the mesos-rack process is still running.</p>
      </ModalButton>
    );
  },

  getColumns(type) {
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
    if (this.typeName[type]) {
      columns.push(
        <Column
          label={this.typeName[type]}
          id="typename"
          key="typename"
          sortable={true}
          sortData={(cellData, rack) => rack.currentState.user || ''}
          cellData={(rack) => this.showUser(rack) && rack.currentState.user}
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
            {this.getMaybeReactivateButton(rack)}
            {this.getDecommissionOrRemoveButton(rack)}
            <JSONButton object={rack}>
              {'{ }'}
            </JSONButton>
          </span>
        )}
      />
    );
    return columns;
  },

  getActiveRacks() {
    return this.props.racks.filter(({currentState}) => Utils.isIn(currentState.state, ['ACTIVE']));
  },

  getDecommissioningRacks() {
    return this.props.racks.filter(({currentState}) => Utils.isIn(currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']));
  },

  getInactiveRacks() {
    return this.props.racks.filter(({currentState}) => Utils.isIn(currentState.state, ['DEAD', 'MISSING_ON_STARTUP']));
  },

  getStates() {
    return [
      {
        stateName: 'Active',
        emptyMessage: 'No Active Racks',
        columns: this.getColumns('active'),
        hostsInState: this.getActiveRacks()
      },
      {
        stateName: 'Decommissioning',
        emptyMessage: 'No Decommissioning Racks',
        columns: this.getColumns('decommissioning'),
        hostsInState: this.getDecommissioningRacks()
      },
      {
        stateName: 'Inactive',
        emptyMessage: 'No Inactive Racks',
        columns: this.getColumns('inactive'),
        hostsInState: this.getInactiveRacks()
      }
    ];
  },

  render() {
    return (
    <MachinesPage
      header = "Racks"
      states = {this.getStates()}
      error = {this.props.error}
    />
    );
  }
});

function getErrorFromState(state) {
  const { decommissionRack, removeRack, reactivateRack } = state.api;
  if (decommissionRack.error) {
    return `Error decommissioning rack: ${ state.api.decommissionRack.error.message }`;
  }
  if (removeRack.error) {
    return `Error removing rack: ${ state.api.removeRack.error.message }`;
  }
  if (reactivateRack.error) {
    return `Error reactivating rack: ${ state.api.reactivateRack.error.message }`;
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
      dispatch(RemoveRack.clear()),
      dispatch(ReactivateRack.clear())
    ]);
  }
  return {
    decommissionRack: (rack, message) => { clear().then(dispatch(DecommissionRack.trigger(rack.id, message))).then(dispatch(FetchRacks.trigger())); },
    removeRack: (rack, message) => { clear().then(dispatch(RemoveRack.trigger(rack.id, message))).then(dispatch(FetchRacks.trigger())); },
    reactivateRack: (rack, message) => { clear().then(dispatch(ReactivateRack.trigger(rack.id, message))).then(dispatch(FetchRacks.trigger())); },
    fetchRacks: () => dispatch(FetchRacks.trigger()),
    clear
  };
}

function refresh(props) {
  return props.fetchRacks();
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(Racks, 'Racks', refresh));
