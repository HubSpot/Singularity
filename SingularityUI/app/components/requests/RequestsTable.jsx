import React, { Component, PropTypes } from 'react';

import RequestPropTypes from '../../constants/api/RequestPropTypes';

import RequestsTableRow from './RequestsTableRow';

class RequestsTable extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsTable';
  }

  render() {
    const rows = this.props.requests
      .slice(0, this.props.maxVisible)
      .map((r) => <RequestsTableRow requestParent={r} key={r.request.id} />);

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
          {rows}
        </tbody>
      </table>
    );
  }
}

RequestsTable.defaultProps = {
  maxVisible: 10
};

RequestsTable.propTypes = {
  requests: PropTypes.arrayOf(RequestPropTypes.RequestParent).isRequired,
  maxVisible: PropTypes.number
}

export default RequestsTable;
