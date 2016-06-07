import React, { Component, PropTypes } from 'react';

class SidebarCheckbox extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'SidebarCheckbox';
  }

  render() {
    return (
      <div className='sidebar-checkbox'>
        <label>
          <input
            type='checkbox'
            checked={this.props.checked}
            onChange={this.props.onChange}
          />
          <span className='checkbox-style' />
        </label>
      </div>
    );
  }
}

SidebarCheckbox.propTypes = {
  checked: PropTypes.bool.isRequired,
  onChange: PropTypes.func.isRequired
}

export default SidebarCheckbox;
