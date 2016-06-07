import React from 'react';

class SearchBar extends React.Component {
  constructor(props) {
    super(props);
    this.displayName = 'SearchBar';
  }

  render() {
    return (
      <div className='form-group has-feedback search-bar'>
        <input className='search-input' type='text' placeholder='Filter requests' />
        <span className='glyphicon glyphicon-search form-control-feedback' />
      </div>
    );
  }
}

export default SearchBar;
