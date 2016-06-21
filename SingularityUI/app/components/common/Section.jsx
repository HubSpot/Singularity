import React from 'react';

export default class Section extends React.Component {

  render() {
    return (
      <div id={this.props.id}>
        <div className="page-header">
            <h2>{this.props.title}</h2>
        </div>
        {this.props.children}
      </div>
    );
  }
}
