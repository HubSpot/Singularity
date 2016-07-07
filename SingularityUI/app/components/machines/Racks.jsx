import React, {PropTypes} from 'react';
import MachinesPage from './MachinesPage';
import PlainText from '../common/atomicDisplayItems/PlainText';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import {Glyphicon} from 'react-bootstrap';
import ModalButton from './ModalButton';
import Utils from '../../utils';
import { connect } from 'react-redux';
import { DecommissionRack, RemoveRack, ReactivateRack } from '../../actions/api/racks';

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

  columnHeads(type) {
    const heads = [
      {
        data: 'ID'
      },
      {
        data: 'Current State'
      },
      {
        data: 'Uptime'
      }
    ];
    if (this.typeName[type]) {
      heads.push({
        data: this.typeName[type]
      });
    }
    heads.push({ data: 'Message' });
    heads.push({}); // Reactivate button and Decommission or Remove button
    return heads;
  },

  getMaybeReactivateButton(rack) {
    return (__in__(rack.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']) &&
      <ModalButton
        buttonChildren={<Glyphicon glyph="new-window" />}
        action="Reactivate Rack"
        onConfirm={(data) => this.props.clear().then(() => this.props.reactivateRack(rack, data.message))}
        tooltipText={`Reactivate ${rack.id}`}>
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
          onConfirm={(data) => this.props.clear().then(() => this.props.decommissionRack(rack, data.message))}
          tooltipText={`Decommission ${rack.id}`}>
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
        onConfirm={(data) => this.props.clear().then(() => this.props.removeRack(rack, data.message))}
        tooltipText={`Remove ${rack.id}`}>
        <p>Are you sure you want to remove this rack??</p>
        <pre>{rack.id}</pre>
        <p>Removing a decommissioned rack will cause that rack to become active again if the mesos-rack process is still running.</p>
      </ModalButton>
    );
  },


  getData(type, rack) {
    const data = [
      {
        component: PlainText,
        prop: {
          text: rack.id
        }
      },
      {
        component: PlainText,
        prop: {
          text: Utils.humanizeText(rack.currentState.state)
        }
      },
      {
        component: TimeStamp,
        prop: {
          display: 'duration',
          timestamp: Date.now() - rack.firstSeenAt
        }
      }
    ];
    if (this.typeName[type]) {
      data.push({
        component: PlainText,
        prop: {
          text: this.showUser(rack) && rack.currentState.user ? rack.currentState.user : ''
        }
      });
    }
    data.push({
      component: PlainText,
      prop: {
        text: rack.currentState.message || ''
      }
    });
    data.push({
      component: PlainText,
      className: 'actions-column',
      prop: {
        text: <div>{this.getMaybeReactivateButton(rack)} {this.getDecommissionOrRemoveButton(rack)} </div>
      }
    });
    return data;
  },

  getRacks(type, racks) {
    return racks.map(rack => {
      return {
        dataId: rack.id,
        data: this.getData(type, rack)
      };
    });
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
        emptyTableMessage: 'No Active Racks',
        stateTableColumnMetadata: this.columnHeads('active'),
        hostsInState: this.getRacks('active', this.getActiveRacks())
      },
      {
        stateName: 'Decommissioning',
        emptyTableMessage: 'No Decommissioning Racks',
        stateTableColumnMetadata: this.columnHeads('decommissioning'),
        hostsInState: this.getRacks('decommissioning', this.getDecommissioningRacks())
      },
      {
        stateName: 'Inactive',
        emptyTableMessage: 'No Inactive Racks',
        stateTableColumnMetadata: this.columnHeads('inactive'),
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
  if (state.api.decommissionRack.error) {
    return `Error decommissioning rack: ${ state.api.decommissionRack.error.message }`;
  }
  if (state.api.removeRack.error) {
    return `Error removing rack: ${ state.api.removeRack.error.message }`;
  }
  if (state.api.reactivateRack.error) {
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
  return {
    decommissionRack: (rack, message) => { dispatch(DecommissionRack.trigger(rack.id, message)); },
    removeRack: (rack, message) => { dispatch(RemoveRack.trigger(rack.id, message)); },
    reactivateRack: (rack, message) => { dispatch(ReactivateRack.trigger(rack.id, message)); },
    clear: () => Promise.all([
      dispatch(DecommissionRack.clear()),
      dispatch(RemoveRack.clear()),
      dispatch(ReactivateRack.clear())
    ])
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Racks);
