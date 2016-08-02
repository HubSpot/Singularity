import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { Log, TailerProvider } from '../src/components';

import { sandboxSetApiRoot } from '../src/actions';

import './example.scss';

class App extends Component {
  constructor() {
    super();
    this.state = {
      taskId: null,
      enteredTaskId: '',
      path: null,
      enteredPath: '',
    };

    this.tailLog = this.tailLog.bind(this);
  }

  componentDidMount() {
    this.props.setSandboxApi(localStorage.apiRootOverride);
  }

  tailLog(e) {
    this.setState({
      taskId: this.state.enteredTaskId,
      path: this.state.enteredPath
    });

    if (e) {
      e.preventDefault();
    }
  }

  render() {
    const { taskId, path } = this.state;
    let maybeLog;

    if (taskId && path) {
      maybeLog = <Log taskId={taskId} path={path} minLines={10} />;
    }

    return (
      <div className="full">
        <div className="app-header">
          <form onSubmit={this.tailLog}>
            <label>
              {'TaskId: '}
              <input
                type="text"
                value={this.state.enteredTaskId}
                onChange={(e) => this.setState({enteredTaskId: e.target.value})}
              />
            </label>
            <label>
              {'Path: '}
              <input
                type="text"
                value={this.state.enteredPath}
                onChange={(e) => this.setState({enteredPath: e.target.value})}
              />
            </label>
            <button type="submit" onClick={this.tailLog}>
              Tail!
            </button>
          </form>
        </div>
        <div className="app-content">
          <TailerProvider getTailerState={(state) => state.tailer}>
            {maybeLog || <div />}
          </TailerProvider>
        </div>
      </div>
    );
  }
}

App.propTypes = {
  setSandboxApi: PropTypes.func.isRequired
};

const mapDispatchToProps = (dispatch) => ({
  setSandboxApi: (uri) => dispatch(sandboxSetApiRoot('/singularity/api'))
});

export default connect(
  null,
  mapDispatchToProps
)(App);
