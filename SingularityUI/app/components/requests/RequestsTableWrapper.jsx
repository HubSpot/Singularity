import React, { Component, PropTypes } from 'react';

import RequestPropTypes from '../../constants/api/RequestPropTypes';

import RequestsTable from './RequestsTable';

class RequestsTableWrapper extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsTableWrapper';
  }
  render() {
    const requestsAPI = this.props.requestsAPI;
    const filteredRequests = this.props.filteredRequests;

    let maybeLoading = <span />;
    let maybeError = <span />;
    let maybeNumberOfRequests = <span />;
    let maybeTable = <span />;

    if (requestsAPI.receivedAt === null) {
      // data has not been received

      // you don't care if it's loading if you already see the data
      if (requestsAPI.isFetching) {
        maybeLoading = <span>Loading...</span>;
      }

      if (requestsAPI.error) {
        maybeError = <span>Error: {requestsAPI.error}</span>;
      }
    } else {
      maybeTable = (
        <RequestsTable
          requests={filteredRequests}
        />
      );
    }

    // todo: maybeNumberOfRequests

    return (
      <div>
        {maybeLoading}
        {maybeError}
        {maybeNumberOfRequests}
        {maybeTable}
      </div>
    );
  }
}

RequestsTableWrapper.propTypes = {
  requestsAPI: PropTypes.shape({
    isFetching: PropTypes.bool.isRequired,
    error: PropTypes.object,
    receivedAt: PropTypes.number,
    data: PropTypes.arrayOf(RequestPropTypes.RequestParent).isRequired
  }).isRequired,
  filteredRequests: PropTypes.arrayOf(RequestPropTypes.RequestParent).isRequired
}

export default RequestsTableWrapper;
