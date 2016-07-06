import React, {PropTypes} from 'react';
import MachinesPage from './MachinesPage';
import PlainText from '../common/atomicDisplayItems/PlainText';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import Link from '../common/atomicDisplayItems/Link';
import Glyphicon from 'react-bootstrap/lib/Glyphicon';
import ModalButton from './ModalButton';
import Utils from '../../utils';
import { connect } from 'react-redux';
import { FreezeSlave, DecommissionSlave, RemoveSlave, ReactivateSlave } from '../../actions/api/slaves';

function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}

class Slaves extends React.Component {

  static propTypes = {
    freezeSlave: PropTypes.func.isRequired,
    decommissionSlave: PropTypes.func.isRequired,
    removeSlave: PropTypes.func.isRequired,
    reactivateSlave: PropTypes.func.isRequired,
    slaves: PropTypes.arrayOf(PropTypes.shape({
      state: PropTypes.string
    }))
  }

  showUser(slave) {
    return __in__(slave.currentState.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']);
  }

  columnHeads(type) {
    const heads = [
      {
        data: 'ID'
      },
      {
        data: 'State'
      },
      {
        data: 'Since'
      },
      {
        data: 'Rack'
      },
      {
        data: 'Host'
      },
      {
        data: 'Uptime',
        className: 'hidden-xs'
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
  }

  getMaybeReactivateButton(slave) {
    return (__in__(slave.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN']) &&
      <ModalButton
        buttonChildren={<Glyphicon glyph="new-window" />}
        action="Reactivate Slave"
        onConfirm={(data) => this.props.reactivateSlave(slave.id, data.message)}
        tooltipText={`Reactivate ${slave.id}`}>
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
        onConfirm={(data) => this.props.freezeSlave(slave.id, data.message)}
        tooltipText={`Freeze ${slave.id}`}>
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
          onConfirm={(data) => this.props.decommissionSlave(slave.id, data.message)}
          tooltipText={`Decommission ${slave.id}`}>
          <p>Are you sure you want to decommission this slave?</p>
          <pre>{slave.id}</pre>
          <p>Decommissioning a slave causes all tasks currently running on it to be rescheduled and executed elsewhere, as new tasks will no longer consider the slave with id <code>{slave.id}</code> a valid target for execution. This process may take time as replacement tasks must be considered healthy before old tasks are killed.</p>
        </ModalButton>
      );
    }
    return (
      <ModalButton
        buttonChildren={<Glyphicon glyph="remove" />}
        action="Remove Slave"
        onConfirm={(data) => this.props.removeSlave(slave.id, data.message)}
        tooltipText={`Remove ${slave.id}`}>
        <p>Are you sure you want to remove this slave?</p>
        <pre>{slave.id}</pre>
        {__in__(slave.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']) &&
        <p>
          Removing a decommissioned slave will cause that slave to become active again if the mesos-slave process is still running.
        </p>}
      </ModalButton>
    );
  }

  getData(type, slave) {
    const now = +new Date();
    const data = [
      {
        component: Link,
        prop: {
          text: slave.id,
          url: `${config.appRoot}/tasks/active/all/${slave.host}`,
          altText: `All tasks running on host ${slave.host}`
        }
      },
      {
        component: PlainText,
        prop: {
          text: Utils.humanizeText(slave.currentState.state)
        }
      },
      {
        component: TimeStamp,
        prop: {
          display: 'absoluteTimestamp',
          timestamp: slave.currentState.timestamp
        }
      },
      {
        component: PlainText,
        prop: {
          text: slave.rackId
        }
      },
      {
        component: PlainText,
        prop: {
          text: slave.host
        }
      },
      {
        component: TimeStamp,
        prop: {
          display: 'duration',
          timestamp: now - slave.firstSeenAt
        }
      }
    ];
    if (this.typeName[type]) {
      data.push({
        component: PlainText,
        prop: {
          text: this.showUser(slave) && slave.currentState.user ? slave.currentState.user : ''
        }
      });
    }
    data.push({
      component: PlainText,
      prop: {
        text: slave.currentState.message || ''
      }
    });
    data.push({
      component: PlainText,
      className: 'actions-column',
      prop: {
        text: <div>
          {this.getMaybeReactivateButton(slave)}
          {this.getMaybeFreezeButton(slave)}
          {this.getDecommissionOrRemoveButton(slave)}
        </div>
      }
    });
    return data;
  }

  getSlaves(type, slaves) {
    const tableifiedSlaves = [];
    slaves.map(slave => {
      return tableifiedSlaves.push({
        dataId: slave.id,
        data: this.getData(type, slave)
      });
    });
    return tableifiedSlaves;
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
        emptyTableMessage: 'No Active Slaves',
        stateTableColumnMetadata: this.columnHeads('active'),
        hostsInState: this.getSlaves('active', this.getActiveSlaves())
      },
      {
        stateName: 'Frozen',
        emptyTableMessage: 'No Frozen Slaves',
        stateTableColumnMetadata: this.columnHeads('frozen'),
        hostsInState: this.getSlaves('decommissioning', this.getFrozenSlaves())
      },
      {
        stateName: 'Decommissioning',
        emptyTableMessage: 'No Decommissioning Slaves',
        stateTableColumnMetadata: this.columnHeads('decommissioning'),
        hostsInState: this.getSlaves('decommissioning', this.getDecommissioningSlaves())
      },
      {
        stateName: 'Inactive',
        emptyTableMessage: 'No Inactive Slaves',
        stateTableColumnMetadata: this.columnHeads('inactive'),
        hostsInState: this.getSlaves('inactive', this.getInactiveSlaves())
      }
    ];
  }

  render() {
    return (
    <MachinesPage
      header = "Slaves"
      states = {this.getStates()}
    />
    );
  }
}

Slaves.prototype.typeName = {
  'active': 'Activated By',
  'frozen': 'Frozen By',
  'decommissioning': 'Decommissioned By'
};

function mapStateToProps(state) {
  return {
    slaves: state.api.slaves.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    freezeSlave: (slave, message) => { dispatch(FreezeSlave.trigger(slave.id, message)); },
    decommissionSlave: (slave, message) => { dispatch(DecommissionSlave.trigger(slave.id, message)); },
    removeSlave: (slave, message) => { dispatch(RemoveSlave.trigger(slave.id, message)); },
    reactivateSlave: (slave, message) => { dispatch(ReactivateSlave.trigger(slave.id, message)); }
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Slaves);
