import React from 'react';
import HostStates from './HostStates';

export default class StatusPage extends React.Component {

  renderState(state, key) {
    return (
      <div key={key}>
          <h2> {state.stateName} </h2>

      </div>
    );
  }

  render() {
    console.log(this.props);
    return (
      <div>
          <HostStates hosts={this.props.model.hostStates} />
      </div>
    );
  }
}

StatusPage.propTypes = {};
