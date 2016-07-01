import React from 'react';

import { Table, Pagination } from 'react-bootstrap';

export default class TasksTable extends React.Component {

  renderTable() {

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
            items={this.props.data.length}
            onSelect={(event, selectedEvent) => this.props.onPage(selectedEvent.eventKey)} />
        </div>
      );
    }
  }

  render() {
    return (
      <div>
        {this.renderPagination()}
      </div>
    );
  }
}
