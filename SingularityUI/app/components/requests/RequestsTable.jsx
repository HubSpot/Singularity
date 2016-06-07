import React, { Component } from 'react';

import RequestsTableRow from './RequestsTableRow';

class RequestsTable extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsTable';
  }

  render() {
    return (
      <table className='table request-table'>

        <colgroup>
          <col span='1' className='column-1' />
          <col span='1' className='column-2' />
          <col span='1' className='column-3' />
          <col span='1' className='column-4' />
          <col span='1' className='column-5' />
          <col span='1' className='column-6' />
        </colgroup>

        <thead>
          <tr className='table-header'>
            <td></td>
            <td></td>
            <td>Active?</td>
            <td>Instances</td>
            <td>Last deployed</td>
            <td></td>
          </tr>
        </thead>

        <tbody>
          <RequestsTableRow />
          <RequestsTableRow />
          <RequestsTableRow />
          <RequestsTableRow />
        </tbody>
      </table>
    );
  }
}

export default RequestsTable;
