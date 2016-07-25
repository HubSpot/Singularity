import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import TailerProvider from '../src/components/TailerProvider';
import { Log } from '../src/components';

import { sandboxSetApiRoot } from '../src/actions';

class App extends Component {
  constructor() {
    super();
    this.state = {
      taskId: null,
      enteredTaskId: ''
    };

    this.tailLog = this.tailLog.bind(this);
  }

  componentDidMount() {
    this.props.setSandboxApi();
  }

  tailLog(e) {
    this.setState({
      taskId: this.state.enteredTaskId
    });

    if (e) {
      e.preventDefault();
    }
  }

  render() {
    return (
      <div>
        <form onSubmit={this.tailLog}>
          <label>
            {'TaskId: '}
            <input
              type="text"
              value={this.state.enteredTaskId}
              onChange={(e) => this.setState({enteredTaskId: e.target.value})}
            />
          </label>
          <button type="submit" onClick={this.tailLog}>
            Tail!
          </button>
        </form>
        <p>{this.state.taskId}</p>
        <TailerProvider getTailerState={(state) => state.tailer}>
          <Log id={this.state.taskId} />
        </TailerProvider>
      </div>
    );
  }
}

App.propTypes = {
  setSandboxApi: PropTypes.func.isRequired
};

const mapDispatchToProps = (dispatch) => ({
  setSandboxApi: () => dispatch(sandboxSetApiRoot('localhost'))
});

export default connect(
  null,
  mapDispatchToProps
)(App);
