import React, { Component } from 'react';

class RequestsTableRow extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsTableRow';
  }

  render() {
    return (
      <tr className='table-row'>
        <td><span className='worker-tag'>worker</span></td>
        <td>
          <a className='request-name' href='#'>SomeRequestName</a>
          <br />
          <span className='subline'>Deployed by <a href='#'>XYZ</a></span>
        </td>
        <td><span className='active-tag'>Active</span></td>
        <td><span className='important-value'>2</span> instances</td>
        <td><span className='important-value'>6</span> minutes ago by <span className='important-value'>mhazlewood</span></td>
        <td>
          <span className='action-links'>
            <a href='#'>Delete</a>|
            <a href='#'>Scale</a>|
            <a href='#'>View JSON</a>|
            <a href='#'>Edit</a>
          </span>
        </td>
      </tr>
    );
  }
}

export default RequestsTableRow;
