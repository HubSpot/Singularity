import React from 'react';
import Table from '../common/Table';

let MachinesPage = React.createClass({

    propTypes: {
        header: React.PropTypes.string.isRequired, // Eg. 'Slaves', 'Racks'
        states: React.PropTypes.arrayOf(React.PropTypes.shape({
            stateName: React.PropTypes.string.isRequired, // Eg. 'Active', 'Frozen', etc
            stateTableColumnMetadata: React.PropTypes.arrayOf(React.PropTypes.shape({
                data: React.PropTypes.string,
                className: React.PropTypes.string,
                doSort: React.PropTypes.func,
                sortable: React.PropTypes.boolean,
                sortAttr: React.PropTypes.string
            })).isRequired,
            // Host info, in a format that can be passed into a table
            hostsInState: React.PropTypes.arrayOf(React.PropTypes.shape({
                dataId: React.PropTypes.string.isRequired,
                className: React.PropTypes.string,
                data: React.PropTypes.arrayOf(React.PropTypes.shape({
                    component: React.PropTypes.func.isRequired,
                    prop: React.PropTypes.object,
                    id: React.PropTypes.string,
                    className: React.PropTypes.string
                })).isRequired
            })).isRequired,
            emptyTableMessage: React.PropTypes.string.isRequired
        })).isRequired
    },

    renderState(state, key) {
      return (
        <div key={key}>
            <h2> {state.stateName} </h2>
            <Table
                noPages = {true}
                tableClassOpts = "table-striped"
                columnHeads = {state.stateTableColumnMetadata}
                tableRows = {state.hostsInState}
                emptyTableMessage = {state.emptyTableMessage}
            />
        </div>
      );
    },

    render() {
      return (
        <div>
            <h1> {this.props.header} </h1>
            {this.props.states.map(this.renderState)}
        </div>
      );
    },
});


export default MachinesPage;
