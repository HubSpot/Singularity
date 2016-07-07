import React, { Component } from 'react';

export default class NotFound extends Component {

  render() {
    return (
      <div>
        <div className="row text-center">
          <h1>Not found</h1>
          <h4>The page you are looking for doesn't exist:</h4>
          <code>{this.props.path}</code>
        </div>
        <div className="row text-center">
          <h4><a href={config.appRoot}>{"Go home"}</a></h4>
        </div>
      </div>
    );
  }
}
