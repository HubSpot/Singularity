import React from 'react';
import MachinesPage from './MachinesPage';
import PlainText from '../common/atomicDisplayItems/PlainText';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import Link from '../common/atomicDisplayItems/Link';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import Utils from '../../utils';
import RacksCollection from '../../collections/Racks';

let Racks = React.createClass({

    typeName: {
        'active': 'Activated By',
        'frozen': 'Frozen By',
        'decommissioning': 'Decommissioned By'
    },

    showUser(rack) {
        return __in__(rack.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']);
    },

    columnHeads(type) {
        let heads = [
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

    refresh() { return this.props.racks.fetch().done(() => this.forceUpdate()); },

    promptReactivate(event, rackModel) {
        event.preventDefault();
        return rackModel.promptReactivate(() => this.refresh());
    },

    promptDecommission(event, rackModel) {
        event.preventDefault();
        return rackModel.promptDecommission(() => this.refresh());
    },

    promptRemove(event, rackModel) {
        event.preventDefault();
        return rackModel.promptRemove(() => this.refresh());
    },

    getMaybeReactivateButton(rackModel) {
        let rack = rackModel.attributes;
        if (__in__(rack.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION'])) {
          return (
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'new-window'
                    />,
                  onClickFn: (event) => { this.promptReactivate(event, rackModel) },
                    title: 'Reactivate',
                    altText: `Reactivate ${rack.id}`,
                    overlayTrigger: true,
                    overlayTriggerPlacement: 'top',
                    overlayToolTipContent: `Reactivate ${rack.id}`,
                    overlayId: `reactivate${rack.id}`
                }}
            />
          );
        } else {
            return null;
        }
    },

    getDecommissionOrRemoveButton(rackModel) {
        let rack = rackModel.attributes;
        if (rack.state === 'ACTIVE') {
          return (
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'trash'
                    />,
                    onClickFn: (event) => { this.promptDecommission(event, rackModel) },
                    title: 'Decommission',
                    altText: `Decommission ${rack.id}`,
                    overlayTrigger: true,
                    overlayTriggerPlacement: 'top',
                    overlayToolTipContent: `Decommission ${rack.id}`,
                    overlayId: `decommission${rack.id}`
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
                    onClickFn: (event) => { this.promptRemove(event, rackModel) },
                    title: 'Remove',
                    altText: `Remove ${rack.id}`,
                    overlayTrigger: true,
                    overlayTriggerPlacement: 'top',
                    overlayToolTipContent: `Remove ${rack.id}`,
                    overlayId: `remove${rack.id}`
                }}
            />
          );
      }
    },


    getData(type, rackModel) {
        let rack = rackModel.attributes;
        let data = [
            {
                component: PlainText,
                prop: {
                    text: rack.id
                }
            },
            {
                component: PlainText,
                prop: {
                    text: Utils.humanizeText(rack.state)
                }
            },
            {
                component: TimeStamp,
                prop: {
                    display: 'duration',
                    timestamp: rack.uptime
                }
            }
        ];
        if (this.typeName[type]) {
            data.push({
                component: PlainText,
                prop: {
                    text: this.showUser(rack) && rack.user ? rack.user : ''
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
                text: <div>{this.getMaybeReactivateButton(rackModel)} {this.getDecommissionOrRemoveButton(rackModel)} </div>
            }
        });
        return data;
    },

    getRacks(type, racks) {
        let tableifiedRacks = [];
        racks.map(rack => {
            return tableifiedRacks.push({
                dataId: rack.id,
                data: this.getData(type, rack)
            });
        });
        return tableifiedRacks;
    },

    getActiveRacks() {
        return new RacksCollection(
            this.props.racks.filter(model => __in__(model.get('state'), ['ACTIVE']))
        );
    },

    getDecommissioningRacks() {
        return new RacksCollection(
            this.props.racks.filter(model => __in__(model.get('state'), ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']))
        );
    },

    getInactiveRacks() {
        return new RacksCollection(
            this.props.racks.filter(model => __in__(model.get('state'), ['DEAD', 'MISSING_ON_STARTUP']))
        );
    },

    getStates() {
        return [
            {
                stateName: "Active",
                emptyTableMessage: "No Active Racks",
                stateTableColumnMetadata: this.columnHeads('active'),
                hostsInState: this.getRacks('active', this.getActiveRacks())
            },
            {
                stateName: "Decommissioning",
                emptyTableMessage: "No Decommissioning Racks",
                stateTableColumnMetadata: this.columnHeads('decommissioning'),
                hostsInState: this.getRacks('decommissioning', this.getDecommissioningRacks())
            },
            {
                stateName: "Inactive",
                emptyTableMessage: "No Inactive Racks",
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
        />
      );
    }
});


export default Racks;

function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}
