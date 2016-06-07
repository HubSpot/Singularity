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
          <input type='checkbox' name={this.props.inputName}/>
          <span className='checkbox-style' />
        </label>
      </div>
    );
  }
}

SidebarCheckbox.propTypes = {
  inputName: PropTypes.string.isRequired
}

export default SidebarCheckbox;
