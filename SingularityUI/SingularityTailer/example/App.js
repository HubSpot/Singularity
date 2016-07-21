import React, { Component } from 'react';

class App extends Component {
  constructor() {
    super();
    this.state = {
      taskId: null,
      enteredTaskId: ''
    };

    this.tailLog = this.tailLog.bind(this);
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
      </div>
    );
  }
}

export default App;
