import React, { Component } from 'react';

class MainContent extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'MainContent';
  }

  render() {
    return (
      <div className='col-lg-10 list-container'>
        {this.props.children}
      </div>
    );
  }
}

export default MainContent;
