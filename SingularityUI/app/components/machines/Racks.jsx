import React from 'react';
import MachinesPage from './MachinesPage';
import PlainText from '../common/atomicDisplayItems/PlainText';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import Link from '../common/atomicDisplayItems/Link';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import Utils from '../../utils';
import { connect } from 'react-redux';

let Racks = React.createClass({

    typeName: {
        'active': 'Activated By',
        'frozen': 'Frozen By',
        'decommissioning': 'Decommissioned By'
    },

    showUser(rack) {
        return __in__(rack.currentState.state, ['ACTIVE', 'DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION']);
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

    getMaybeReactivateButton(rack) {
        if (__in__(rack.currentState.state, ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION'])) {
          return (
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'new-window'
                    />,
                  onClickFn: (event) => { this.promptReactivate(event, rack) },
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

    getDecommissionOrRemoveButton(rack) {
        if (rack.currentState.state === 'ACTIVE') {
          return (
            <Link
                prop = {{
                    text: <Glyphicon
                        iconClass = 'trash'
                    />,
                    onClickFn: (event) => { this.promptDecommission(event, rack) },
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
                    onClickFn: (event) => { this.promptRemove(event, rack) },
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


    getData(type, rack) {
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
                    text: Utils.humanizeText(rack.currentState.state)
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

function mapStateToProps(state) {
    return {
        racks: state.api.racks.data
    };
}

export default connect(mapStateToProps)(Racks);

function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}
