import React from 'react';

import Table from 'react-bootstrap/lib/Table';
import Pagination from 'react-bootstrap/lib/Pagination';

// NOTE: This component is a temperary fill-in until the official dynamic table is completed.
export default class SimpleTable extends React.Component {

  constructor() {
    super();
    this.state = {
      activePage: 1
    };
  }

  handleSelect(event, selectedEvent) {
    this.setState({
      activePage: selectedEvent.eventKey
    });
  }

  renderTableRows() {
    let page = this.state.activePage - 1;
    let items = this.props.perPage;
    let entries = this.props.entries.slice(page * items, (page * items) + items);
    const rows = entries.map((e, i) => {
      return this.props.renderTableRow(e, i);
    });
    return rows;
  }

  renderPagination() {
    if (this.props.entries.length > this.props.perPage) {
      return (
        <div className="pagination-container">
          <Pagination
            prev={true}
            next={true}
            first={true}
            last={true}
            ellipsis={false}
            items={Math.ceil(this.props.entries.length / this.props.perPage)}
            maxButtons={2}
            activePage={this.state.activePage}
            onSelect={this.handleSelect.bind(this)} />
        </div>
      );
    }
  }

  render() {
    if (this.props.entries.length > 0) {
      return (
        <div className="table-container">
          <Table responsive>
            <thead>
              {this.props.renderTableHeaders()}
            </thead>
            <tbody>
              {this.renderTableRows()}
            </tbody>
          </Table>
          {this.renderPagination()}
        </div>
      );
    } else {
      return (
        <div className="empty-table-message">
            No {this.props.unit}s
        </div>
      );
    }
  }
}
