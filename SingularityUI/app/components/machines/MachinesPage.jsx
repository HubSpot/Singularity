import React from 'react';
import UITable from '../common/table/UITable';

function MachinesPage (props) {
  function renderState(state, key) {
    return (
    <div key={key}>
      <h2> {state.stateName} </h2>
      <UITable
        emptyTableMessage={state.emptyMessage}
        data={state.hostsInState}
        keyGetter={(slave) => slave.id}
        rowChunkSize={20}
        paginated={state.paginated}
      >
        {state.columns}
      </UITable>
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
    hostsInState: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    emptyMessage: React.PropTypes.string.isRequired,
    columns: React.PropTypes.arrayOf(React.PropTypes.node).isRequired,
    paginated: React.PropTypes.bool.isRequired
  })).isRequired
};

export default MachinesPage;
