import React, { Component, PropTypes } from 'react';

import SidebarCheckbox from './SidebarCheckbox';

class SidebarFilterOption extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'SidebarFilterOption';

    this.defaultProps = {
      indicatorClass: ''
    }
  }

  render() {
    return (
      <li>
        <div>
          <span className={this.props.indicatorClass} />
          {this.props.label}
          <SidebarCheckbox
            checked={this.props.isEnabled}
            onChange={this.props.onChange}
          />
          {` (${this.props.numberOfItems})`}
        </div>
      </li>
    );
  }
}

SidebarFilterOption.propTypes = {
  label: PropTypes.string.isRequired,
  isEnabled: PropTypes.bool.isRequired,
  numberOfItems: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  indicatorClass: PropTypes.string
}

export default SidebarFilterOption;
