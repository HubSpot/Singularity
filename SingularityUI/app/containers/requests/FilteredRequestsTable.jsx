import React, { PropTypes } from 'react';
import { connect } from 'react-redux'
import classNames from 'classnames';

import * as RequestsPageActions from '../../actions/ui/requestsPage';

import RequestsTableWrapper from '../../components/requests/RequestsTableWrapper';

const mapStateToProps = (state, ownProps) => {
  const requestsPage = state.ui.requestsPage;
  const requestsAPI = state.api.requests;

  let filteredRequests = requestsAPI.data;

  // filter by type
  if (requestsPage.typeFilter !== 'ALL') {
    filteredRequests = filteredRequests.filter((r) => {
      return requestsPage.typeFilter === r.request.requestType;
    });
  }

  // filter by state
  filteredRequests = filteredRequests.filter((r) => {
    return requestsPage.stateFilter.indexOf(r.state) > -1;
  });

  // filter by text
  // todo: ^

  return {
    filteredRequests: filteredRequests,
    requestsAPI: requestsAPI,
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
