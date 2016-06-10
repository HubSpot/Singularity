import React, { PropTypes } from 'react';
import { connect } from 'react-redux'
import classNames from 'classnames';

import * as RequestsPageActions from '../../actions/ui/requestsPage';

import SearchBar from '../../components/common/SearchBar';

const mapStateToProps = (state, ownProps) => {
  return {
    value: state.ui.requestsPage.textFilter
  };
};

const mapDispatchToProps = (dispatch, ownProps) => {
  return {
    onChange: (e) => dispatch(RequestsPageActions.changeTextFilter(e.target.value))
  };
};

const FilterSearchBar = connect(
  mapStateToProps,
  mapDispatchToProps
)(SearchBar);

export default FilterSearchBar;
