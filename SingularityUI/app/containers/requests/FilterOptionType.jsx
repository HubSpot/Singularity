import React, { PropTypes } from 'react';
import { connect } from 'react-redux'
import classNames from 'classnames';

import * as RequestsPageActions from '../../actions/ui/requestsPage';

import TabBarFilterOption from '../../components/common/tabBar/TabBarFilterOption';

const mapStateToProps = (state, ownProps) => {
  const requestsPage = state.ui.requestsPage;
  const requestsAPI = state.api.requests;

  let numberOfItems;
  if (ownProps.filterValue === 'ALL') {
    numberOfItems = requestsAPI.data.length;
  } else {
    numberOfItems = requestsAPI.data.reduce(
      (count, r) => {
        return count + (ownProps.filterValue === r.request.requestType ? 1 : 0);
      },
      0
    );
  }

  return {
    label: ownProps.label,
    isEnabled: requestsPage.typeFilter === ownProps.filterValue,
    numberOfItems: numberOfItems
  };
};

const mapDispatchToProps = (dispatch, ownProps) => {
  return {
    onClick: () => {
      dispatch(RequestsPageActions.changeTypeFilter(ownProps.filterValue));
    }
  };
};

const FilterOptionType = connect(
  mapStateToProps,
  mapDispatchToProps
)(TabBarFilterOption);

FilterOptionType.propTypes = {
  label: PropTypes.string.isRequired,
  filterValue: PropTypes.string.isRequired
};

export default FilterOptionType;
