import React, { Component } from 'react';

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
          <tr className='table-row'>
            <td><span className='worker-tag'>worker</span></td>
            <td><a href='#'>SomeRequestName</a><br /><span className='subline'>Deployed by <a href='#'>XYZ</a></span></td>
            <td><span className='active-tag'>Active</span></td>
            <td><span className='important-value'>2</span> instances</td>
            <td><span className='important-value'>6</span> minutes ago by <span className='important-value'>mhazlewood</span></td>
            <td>
              <span className='action-links'>
                <a href='#'>Delete</a> |
                <a href='#'>Scale</a> |
                <a href='#'>View JSON</a> |
                <a href='#'>Edit</a>
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    );
  }
}

export default RequestsTable;
