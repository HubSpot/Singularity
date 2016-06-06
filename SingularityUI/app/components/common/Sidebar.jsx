import React, { Component } from 'react';

class Sidebar extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'Sidebar';
  }

  render() {
    return (
      <div className='col-lg-2 filter-container'>
        <ul>
          <li className='active'><div><strong>All states</strong></div></li>
          <li>
            <ul>
              {this.props.children}
            </ul>
          </li>
        </ul>
      </div>
    );
  }
}

export default Sidebar;
