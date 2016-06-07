import React from 'react';
import MachinesPage from './MachinesPage';
import PlainText from '../common/atomicDisplayItems/PlainText';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import Link from '../common/atomicDisplayItems/Link';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import Utils from '../../utils';
import SlavesCollection from '../../collections/Slaves';

let Slaves = React.createClass({

    typeName: {
        'active': 'Activated By',
        'frozen': 'Frozen By',
        'decommissioning': 'Decommissioned By'
    },

    showUser(slave) {
        return __in__(slave.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']);
    },

    columnHeads(type) {
        let heads = [
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
    },

    refresh() { return this.props.slaves.fetch().done(() => this.forceUpdate()); },

    promptReactivate(event, slaveModel) {
        event.preventDefault();
        return slaveModel.promptReactivate(() => this.refresh());
    },

    promptDecommission(event, slaveModel) {
        event.preventDefault();
        return slaveModel.promptDecommission(() => this.refresh());
    },

    promptFreeze(event, slaveModel) {
        event.preventDefault();
        return slaveModel.promptFreeze(() => this.refresh());
    },

    promptRemove(event, slaveModel) {
        event.preventDefault();
        return slaveModel.promptRemove(() => this.refresh());
    },

    getMaybeReactivateButton(slaveModel) {
        let slave = slaveModel.attributes;
        if (__in__(slave.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'FROZEN'])) {
            return (
              <Link
                  prop = {{
                      text: <Glyphicon
                          iconClass = 'new-window'
                      />,
                      onClickFn: (event) => {this.promptReactivate(event, slaveModel)},
                      title: 'Reactivate',
                      altText: `Reactivate ${slave.id}`,
                      overlayTrigger: {true},
                      overlayTriggerPlacement: 'top',
                      overlayToolTipContent: `Reactivate ${slave.id}`,
                      overlayId: `reactivate${slave.id}`,
                  }}
              />
            );
        } else {
            return null;
        }
    },

    getMaybeFreezeButton(slaveModel) {
        let slave = slaveModel.attributes;
        if (slave.state === 'ACTIVE') {
          return (
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'stop'
                    />,
                    onClickFn: (event) => {this.promptFreeze(event, slaveModel)},
                    title: 'Freeze',
                    altText: `Freeze ${slave.id}`,
                    overlayTrigger: true,
                    overlayTriggerPlacement: 'top',
                    overlayToolTipContent: `Freeze ${slave.id}`,
                    overlayId: `freeze${slave.id}`
                }}
            />
          );
        } else {
            return null;
        }
    },

    getDecommissionOrRemoveButton(slaveModel) {
        let slave = slaveModel.attributes;
        if (__in__(slave.state, ['ACTIVE', 'FROZEN'])) {
          return (
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'trash'
                    />,
                    onClickFn: (event) => {this.promptDecommission(event, slaveModel)},
                    title: 'Decommission',
                    altText: `Decommission ${slave.id}`,
                    overlayTrigger: true,
                    overlayTriggerPlacement: 'top',
                    overlayToolTipContent: `Decommission ${slave.id}`,
                    overlayId: `decommission${slave.id}`
                }}
            />
          );
        } else {
          return (
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'remove'
                    />,
                    onClickFn: (event) => {this.promptRemove(event, slaveModel)},
                    title: 'Remove',
                    altText: `Remove ${slave.id}`,
                    overlayTrigger: true,
                    overlayTriggerPlacement: 'top',
                    overlayToolTipContent: `Remove ${slave.id}`,
                    overlayId: `remove${slave.id}`,
                }}
            />
          );
      }
    },

    getData(type, slaveModel) {
        let slave = slaveModel.attributes;
        let data = [
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
                    text: Utils.humanizeText(slave.state)
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
                    timestamp: slave.uptime
                }
            }
        ];
        if (this.typeName[type]) {
            data.push({
                component: PlainText,
                prop: {
                    text: this.showUser(slave) && slave.user ? slave.user : ''
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
                    {this.getMaybeReactivateButton(slaveModel)}
                    {this.getMaybeFreezeButton(slaveModel)}
                    {this.getDecommissionOrRemoveButton(slaveModel)}
                </div>
            }
        });
        return data;
    },

    getSlaves(type, slaves) {
        let tableifiedSlaves = [];
        slaves.map(slave => {
            return tableifiedSlaves.push({
                dataId: slave.id,
                data: this.getData(type, slave)
            });
        });
        return tableifiedSlaves;
    },

    getActiveSlaves() {
        return new SlavesCollection(
            this.props.slaves.filter(model => __in__(model.get('state'), ['ACTIVE']))
        );
    },

    getFrozenSlaves() {
        return new SlavesCollection(
            this.props.slaves.filter(model => __in__(model.get('state'), ['FROZEN']))
        );
    },

    getDecommissioningSlaves() {
        return new SlavesCollection(
            this.props.slaves.filter(model => __in__(model.get('state'), ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']))
        );
    },

    getInactiveSlaves() {
        return new SlavesCollection(
            this.props.slaves.filter(model => __in__(model.get('state'), ['DEAD', 'MISSING_ON_STARTUP']))
        );
    },

    getStates() {
        return [
            {
                stateName: "Active",
                emptyTableMessage: "No Active Slaves",
                stateTableColumnMetadata: this.columnHeads('active'),
                hostsInState: this.getSlaves('active', this.getActiveSlaves())
            },
            {
                stateName: "Frozen",
                emptyTableMessage: "No Frozen Slaves",
                stateTableColumnMetadata: this.columnHeads('frozen'),
                hostsInState: this.getSlaves('decommissioning', this.getFrozenSlaves())
            },
            {
                stateName: "Decommissioning",
                emptyTableMessage: "No Decommissioning Slaves",
                stateTableColumnMetadata: this.columnHeads('decommissioning'),
                hostsInState: this.getSlaves('decommissioning', this.getDecommissioningSlaves())
            },
            {
                stateName: "Inactive",
                emptyTableMessage: "No Inactive Slaves",
                stateTableColumnMetadata: this.columnHeads('inactive'),
                hostsInState: this.getSlaves('inactive', this.getInactiveSlaves())
            }
        ];
    },

    render() {
      return (
        <MachinesPage
            header = "Slaves"
            states = {this.getStates()}
        />
      );
    }
});


export default Slaves;

function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}
