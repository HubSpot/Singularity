import React, { Component } from 'react';

class RequestCounts extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestCounts';
  }

  render() {
    return (
      <div className='row'>
        <div className='col-md-12'>
          <div className='page-header'>
            <h2>My requests</h2>
          </div>
          <div className='row'>
            {this.props.children.map((well) => <div key={well.props.label} className='col-md-2'>{well}</div>)}
          </div>
        </div>
      </div>
    );
  }
}

export default RequestCounts;
