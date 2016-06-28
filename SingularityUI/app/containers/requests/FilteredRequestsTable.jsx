import React, { PropTypes } from 'react';
import { connect } from 'react-redux'

import * as RequestsPageActions from '../../actions/ui/requestsPage';

import { getFilteredRequests } from '../../selectors/api';

import RequestsTableWrapper from '../../components/requests/RequestsTableWrapper';

const mapStateToProps = (state, ownProps) => {
  return {
    filteredRequests: getFilteredRequests(state),
    requestsAPI: state.api.requests,
  };
};

const mapDispatchToProps = (dispatch, ownProps) => {
  return {
  };
};

const FilteredRequestsTable = connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestsTableWrapper);

export default FilteredRequestsTable;
