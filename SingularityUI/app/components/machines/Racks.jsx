import React, {PropTypes} from 'react';
import MachinesPage from './MachinesPage';
import {Glyphicon} from 'react-bootstrap';
import ModalButton from './ModalButton';
import MessageElement from './MessageElement';
import Utils from '../../utils';
import { connect } from 'react-redux';
import { DecommissionRack, RemoveRack, ReactivateRack, FetchRacks } from '../../actions/api/racks';

function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}

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
    return __in__(rack.currentState.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']);
  },

  getMaybeReactivateButton(rack) {
    return (__in__(rack.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']) &&
      <ModalButton
        buttonChildren={<Glyphicon glyph="new-window" />}
        action="Reactivate Rack"
        onConfirm={(data) => this.props.reactivateRack(rack, data.message)}
        tooltipText={`Reactivate ${rack.id}`}
        formElements={[MessageElement]}>
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
          formElements={[MessageElement]}>
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
        formElements={[MessageElement]}>
        <p>Are you sure you want to remove this rack??</p>
        <pre>{rack.id}</pre>
        <p>Removing a decommissioned rack will cause that rack to become active again if the mesos-rack process is still running.</p>
      </ModalButton>
    );
  },

  columnHeads(type) {
    const heads = ['ID', 'Current State', 'Uptime'];
    if (this.typeName[type]) {
      heads.push(this.typeName[type]);
    }
    heads.push('Message');
    heads.push(''); // Reactivate button and Decommission or Remove button
    return heads;
  },


  getRow(type, rack) {
    return (
      <tr key={rack.id}>
        <td>{rack.id}</td>
        <td>{Utils.humanizeText(rack.currentState.state)}</td>
        <td>{Utils.duration(Date.now() - rack.firstSeenAt)}</td>
        {this.typeName[type] && <td>{this.showUser(rack) && rack.currentState.user}</td>}
        <td>{rack.currentState.message}</td>
        <td className="actions-column">
          {this.getMaybeReactivateButton(rack)}
          {this.getDecommissionOrRemoveButton(rack)}
        </td>
      </tr>
    );
  },

  getRacks(type, racks) {
    return racks.map(rack => this.getRow(type, rack));
  },

  getActiveRacks() {
    return this.props.racks.filter(({currentState}) => __in__(currentState.state, ['ACTIVE']));
  },

  getDecommissioningRacks() {
    return this.props.racks.filter(({currentState}) => __in__(currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']));
  },

  getInactiveRacks() {
    return this.props.racks.filter(({currentState}) => __in__(currentState.state, ['DEAD', 'MISSING_ON_STARTUP']));
  },

  getStates() {
    return [
      {
        stateName: 'Active',
        emptyMessage: 'No Active Racks',
        headers: this.columnHeads('active'),
        hostsInState: this.getRacks('active', this.getActiveRacks())
      },
      {
        stateName: 'Decommissioning',
        emptyMessage: 'No Decommissioning Racks',
        headers: this.columnHeads('decommissioning'),
        hostsInState: this.getRacks('decommissioning', this.getDecommissioningRacks())
      },
      {
        stateName: 'Inactive',
        emptyMessage: 'No Inactive Racks',
        headers: this.columnHeads('inactive'),
        hostsInState: this.getRacks('inactive', this.getInactiveRacks())
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
    clear
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Racks);
