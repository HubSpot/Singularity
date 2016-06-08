import React, { PropTypes } from 'react';
import { connect } from 'react-redux'
import classNames from 'classnames';

import * as RequestsPageActions from '../../actions/ui/requestsPage';

import SidebarFilterOption from '../../components/common/sidebar/SidebarFilterOption';

const lookupIndicatorClass = (filterValue) => {
  return classNames({
    'state-indicator': true,
    'active': filterValue === 'ACTIVE',
    'cooling-down': filterValue === 'SYSTEM_COOLDOWN',
    'paused': filterValue === 'PAUSED'
  });
};

const mapStateToProps = (state, ownProps) => {
  const requestsPage = state.requestsPage;
  const requests = state.requests;

  return {
    label: ownProps.label,
    isEnabled: requestsPage.stateFilter.indexOf(ownProps.filterValue) > -1,
    numberOfItems: requests.data.reduce((count, r) => {
      return count + (r.state === ownProps.filterValue ? 1 : 0);
    }, 0),
    indicatorClass: lookupIndicatorClass(ownProps.filterValue)
  };
};

const mapDispatchToProps = (dispatch, ownProps) => {
  return {
    onChange: () => {
      dispatch(RequestsPageActions.toggleStateFilter(ownProps.filterValue));
    }
  };
};

const FilterOptionState = connect(
  mapStateToProps,
  mapDispatchToProps
)(SidebarFilterOption);

FilterOptionState.propTypes = {
  label: PropTypes.string.isRequired,
  filterValue: PropTypes.string.isRequired
};

export default FilterOptionState;
