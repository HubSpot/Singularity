import React, { Component, PropTypes } from 'react';

class SearchBar extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'SearchBar';
  }

  render() {
    return (
      <div className='form-group has-feedback search-bar'>
        <input
          className='search-input'
          type='text'
          placeholder='Find a request'
          value={this.props.value}
          onChange={this.props.onChange}
        />
        <span className='glyphicon glyphicon-search form-control-feedback' />
      </div>
    );
  }
}

SearchBar.propTypes = {
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired
};

export default SearchBar;
