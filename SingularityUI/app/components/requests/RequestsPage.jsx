import React from 'react';
import { connect } from 'react-redux';

import { FetchAction } from '../../actions/api/requests';
import { RemoveAction } from '../../actions/api/request';

import UITable from '../common/table/UITable';
import RequestFilters from './RequestFilters';
import * as Cols from './Columns';

import Utils from '../../utils';

class RequestsPage extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      filter: {
        subFilter: props.subFilter,
        searchFilter: props.searchFilter,
        loading: false
      }
    }
  }

  componentDidMount() {
    Utils.fixTableColumns($(this.refs.table.getTableDOMNode()));
  }

  getColumns() {
    switch(this.state.filter.subFilter) {
      case RequestFilters.REQUEST_TYPES.ALL:
        return [Cols.RequestId, Cols.Type, Cols.State, Cols.Instances, Cols.DeployId, Cols.DeployUser, Cols.LastDeploy, Cols.Schedule, Cols.Actions(this.props.removeRequest)];
    }
  }

  render() {
    console.log(_.filter(this.props.requests, (r) => r.request.requestType == 'SCHEDULED'));

    const displayRequests = this.props.requests;

    let table;
    if (this.state.loading) {
      table = <div className="page-loader fixed"></div>;
    }
    else if (!displayRequests.length) {
      table = <div className="empty-table-message"><p>No matching tasks</p></div>;
    } else {
      table = (
        <UITable
          ref="table"
          data={displayRequests}
          keyGetter={(r) => r.request.id}
        >
          {this.getColumns()}
        </UITable>
      );
    }

    return (
      <div>
        {table}
      </div>
    );
  }
}

function mapStateToProps(state, ownProps) {
  return {
    requests: state.api.requests.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchFilter: (state) => dispatch(FetchAction.trigger(state)),
    removeRequest: (requestid, data) => dispatch(RemoveAction.trigger(requestid, data)),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(RequestsPage);
