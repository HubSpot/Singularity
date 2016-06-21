import React, { Component, PropTypes } from 'react';

import UITable from '../common/table/UITable';
import { RequestId, Type, LastDeploy, DeployUser, State } from './Columns';

class RequestsTable extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsTable';

    this.state = {
      sortBy: 'requestId',
      sortDirection: UITable.SortDirection.ASC
    }
  }

  render() {
    return (
      <UITable
        data={this.props.requests}
        keyGetter={(r) => r.request.id}
        asyncSort
        paginated
      >
        {RequestId}
        {Type}
        {LastDeploy}
        {DeployUser}
        {State}
      </UITable>
    );
  }
}

export default RequestsTable;
