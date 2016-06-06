import React, { Component } from 'react';

class TabBar extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'TabBar';
  }

  render() {
    return (
      <ul className='nav nav-tabs'>
        {this.props.children}
      </ul>
    );
  }
}

export default TabBar;
