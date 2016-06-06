import React from 'react';

class SearchBar extends React.Component {
  constructor(props) {
    super(props);
    this.displayName = 'SearchBar';
  }

  render() {
    return <input className='filter-input' type='text' placeholder='Filter requests' />;
  }
}

export default SearchBar;
