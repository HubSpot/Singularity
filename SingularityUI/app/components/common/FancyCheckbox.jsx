import React, { Component, PropTypes } from 'react';

class FancyCheckbox extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'FancyCheckbox';
  }

  render() {
    return (
      <div className='fancy-checkbox'>
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

FancyCheckbox.propTypes = {
  checked: PropTypes.bool.isRequired,
  onChange: PropTypes.func.isRequired
}

export default FancyCheckbox;
