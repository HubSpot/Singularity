import React, { Component, PropTypes } from 'react';

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
          {this.props.filterName}
          <input
            type='checkbox'
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
  isEnabled: PropTypes.bool.isRequired,
  filterName: PropTypes.string.isRequired,
  numberOfItems: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  indicatorClass: PropTypes.string
}

export default SidebarFilterOption;
