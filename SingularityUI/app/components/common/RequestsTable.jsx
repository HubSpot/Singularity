import React, { Component, PropTypes } from 'react';
import moment from 'moment';

import UITable from './table/UITable';
import Column from './table/Column';

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
        <Column
          label='Request'
          id='requestId'
          cellData={
            (rowData) => rowData.request.id
          }
          sortable
        />
        <Column
          label='Type'
          id='type'
          cellData={
            (rowData) => rowData.request.requestType
          }
          sortable
        />
        <Column
          label='Time of Last Deploy'
          id='lastDeploy'
          cellData={
            (rowData) => {
              if ('requestDeployState' in rowData) {
                const requestDeployState = rowData.requestDeployState;
                if ('activeDeploy' in requestDeployState) {
                  const activeDeploy = requestDeployState.activeDeploy;
                  return activeDeploy.timestamp;
                }
              }
              return null;
            }
          }
          cellRender={
            (cellData) => {
              if (cellData !== null) {
                const fromNow = moment(cellData).fromNow();
                const when = moment(cellData).calendar();
                return `${fromNow} (${when})`;
              }
              return '';
            }
          }
          sortable
        />
        <Column
          label='Status'
          id='state'
          cellData={
            (rowData) => rowData.state
          }
          sortable
        />
        <Column
          label='Deploy User'
          id='user'
          cellData={
            (rowData) => {
              if ('requestDeployState' in rowData) {
                if ('activeDeploy' in rowData.requestDeployState) {
                  if ('user' in rowData.requestDeployState.activeDeploy) {
                    const user = rowData.requestDeployState.activeDeploy.user;
                    // assume user is an email address
                    return user.split('@')[0];
                  }
                }
              }
              return '';
            }
          }
          sortable
        />
      </UITable>
    );
  }
}

export default RequestsTable;
