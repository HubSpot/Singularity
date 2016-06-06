import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';

class TabBarFilterOption extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'TabBarFilterOption';

    this.defaultProps = {
      indicatorClass: ''
    }
  }

  render() {
    const classes = classNames({
      active: this.props.isEnabled
    });

    return (
      <li className={classes}>
        <a href={'#'}>
          {this.props.filterName}
          {` (${this.props.numberOfItems})`}
        </a>
      </li>
    );
  }
}

TabBarFilterOption.propTypes = {
  isEnabled: PropTypes.bool.isRequired,
  filterName: PropTypes.string.isRequired,
  numberOfItems: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
}

export default TabBarFilterOption;
