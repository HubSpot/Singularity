import React from 'react';
import OldTable from '../common/OldTable';

function MachinesPage (props) {
  function renderState(state, key) {
    return (
    <div key={key}>
      <h2> {state.stateName} </h2>
      <OldTable
        noPages = {true}
        tableClassOpts = "table-striped"
        columnHeads = {state.stateTableColumnMetadata}
        tableRows = {state.hostsInState}
        emptyTableMessage = {state.emptyTableMessage}
      />
    </div>
    );
  }

  return (
    <div>
      {props.error &&
        <p className="alert alert-danger">
          {props.error}
        </p>
      }
      <h1> {props.header} </h1>
      {props.states.map(renderState)}
    </div>
  );
}

MachinesPage.propTypes = {
  error: React.PropTypes.string,
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
};

export default MachinesPage;
