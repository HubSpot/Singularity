import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import RequestsTable from '../common/RequestsTable';


class DashboardPage extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'DashboardPage';
  }

  render() {
    return (
      <div>
        <RequestsTable
          requests={this.props.requests}
        />
      </div>
    );
  }
}

RequestsTable.propTypes = {
  requests: PropTypes.arrayOf(PropTypes.object).isRequired
};

const mapStateToProps = (state) => {
  const requestsAPI = state.api.requests;

  return {
    requests: requestsAPI.data
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
  };
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DashboardPage);
