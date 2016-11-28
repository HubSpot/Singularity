import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { withRouter, Link } from 'react-router';

import { TailerProvider } from '../src/components';

import { sandboxSetApiRoot } from '../src/actions';

import './example.scss';

import '../src/styles/ansi.scss';
import '../src/styles/index.scss';

class App extends Component {
  constructor() {
    super();

    this.state = {
      enteredTaskId: '',
      enteredPath: '',
    };

    this.tailLog = this.tailLog.bind(this);
  }

  componentWillMount() {
    this.props.setSandboxApi(localStorage.apiRootOverride);
  }

  tailLog(e) {
    const taskId = this.state.enteredTaskId;
    const path = this.state.enteredPath;

    this.props.router.push(`/${taskId}/tail/${path}`);

    if (e) {
      e.preventDefault();
    }
  }

  render() {
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
          <Link to="/test">Mocha Tests</Link>
        </div>
        <div className="app-content">
          <TailerProvider getTailerState={(state) => state.tailer}>
            {this.props.children || <div />}
          </TailerProvider>
        </div>
      </div>
    );
  }
}

App.propTypes = {
  setSandboxApi: PropTypes.func.isRequired,
  children: PropTypes.node,
  router: PropTypes.object.isRequired
};

const mapDispatchToProps = (dispatch) => ({
  setSandboxApi: (uri) => dispatch(sandboxSetApiRoot('/singularity/api'))
});

export default withRouter(connect(
  null,
  mapDispatchToProps
)(App));
