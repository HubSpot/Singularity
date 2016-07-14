import React from 'react';
import SimpleTable from '../common/SimpleTable';

function MachinesPage (props) {
  function renderState(state, key) {
    return (
    <div key={key}>
      <h2> {state.stateName} </h2>
      <SimpleTable
        entries={state.hostsInState}
        emptyMessage={state.emptyMessage}
        headers={state.headers}
        perPage={20}
        renderTableRow={(machine) => machine}
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
    headers: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
    hostsInState: React.PropTypes.arrayOf(React.PropTypes.node).isRequired,
    emptyMessage: React.PropTypes.string.isRequired
  })).isRequired
};

export default MachinesPage;
