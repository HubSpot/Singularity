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
      <UITable data={this.props.requests} keyGetter={(r) => r.request.id}>
        <Column
          label='Request'
          id='requestId'
          cellData={
            (rowData) => rowData.request.id
          }
          className={'good'}
        />
        <Column
          label='Type'
          id='type'
          cellData={
            (rowData) => rowData.request.requestType
          }
        />
        <Column
          label='Time of Last Deploy'
          id='lastDeploy'
          cellData={
            (rowData) => {
              if ('requestDeployState' in rowData) {
                if ('activeDeploy' in rowData.requestDeployState) {
                  const fromNow = moment(rowData.requestDeployState.activeDeploy.timestamp).fromNow();
                  const when = moment(rowData.requestDeployState.activeDeploy.timestamp).calendar();
                  return `${fromNow} (${when})`;
                }
              }
              return '';
            }
          }
        />
        <Column
          label='Status'
          id='state'
          cellData={
            (rowData) => rowData.state
          }
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
                    return user.split('@')[0];
                  }
                }
              }
              return '';
            }
          }
        />
      </UITable>
    );
  }
}

export default RequestsTable;
