import React from 'react';

import { Table, Pagination } from 'react-bootstrap';

export default class TasksTable extends React.Component {

  renderHeaders() {
    let row = this.props.headers.map((h, i) => {
      return <th key={i}>{h}</th>;
    });
    return <tr>{row}</tr>;
  }

  renderTableRows() {
    const rows = this.props.data.map((e, i) => {
      return this.props.renderTableRow(e, i);
    });
    return rows;
  }

  renderPagination() {
    if (this.props.paginate) {
      return (
        <div className="pagination-container">
          <Pagination
            prev
            next
            maxButtons={1}
            ellipsis={false}
            activePage={this.props.page}
            items={this.props.page + (this.props.disableNext ? 0 : 1)}
            onSelect={(event, selectedEvent) => this.props.onPage(selectedEvent.eventKey)} />
        </div>
      );
    }
  }

  renderTable() {
    return (
      <div>
        <Table responsive striped>
          <thead>
            {this.renderHeaders()}
          </thead>
          <tbody>
            {this.renderTableRows()}
          </tbody>
        </Table>
      </div>
    );
  }

  render() {
    if (this.props.data.length) {
      return (
        <div className="table-container">
          {this.renderTable()}
          {this.renderPagination()}
        </div>
      );
    } else {
      return (
        <div className="empty-table-message">
            {this.props.emptyMessage}
        </div>
      );
    }
  }
}
