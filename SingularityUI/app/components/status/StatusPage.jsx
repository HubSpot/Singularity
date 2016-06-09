import React from 'react';

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
          <h1>Status</h1>

      </div>
    );
  }
}

StatusPage.propTypes = {

};
