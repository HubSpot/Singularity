import React from 'react';

import Table from 'react-bootstrap/lib/Table';
import Pagination from 'react-bootstrap/lib/Pagination';

export default class SimpleTable extends React.Component {

  constructor(props) {
    super();
    this.state = {
      activePage: 1,
      displayItems: props.entries
    };
  }

  componentWillReceiveProps(nextProps) {
    this.updateDisplay(nextProps);
  }

  updateDisplay(nextProps) {
    this.setState({
      displayItems: nextProps.entries
    });
  }

  handleSelect(event) {
    this.setState({
      activePage: event
    });
  }

  renderTableRows() {
    const page = this.state.activePage - 1;
    const items = this.props.perPage;
    const entries = this.state.displayItems.slice(page * items, (page * items) + items);
    const rows = entries.map((entry, key) => {
      return this.props.renderTableRow(entry, key);
    });
    return rows;
  }

  renderPagination() {
    return (this.state.displayItems.length > this.props.perPage) && (
      <div className="pagination-container">
        <Pagination
          prev={true}
          next={true}
          first={this.props.first}
          last={this.props.last}
          ellipsis={false}
          items={Math.ceil(this.state.displayItems.length / this.props.perPage)}
          maxButtons={this.props.maxButtons || 1}
          activePage={this.state.activePage}
          onSelect={(event) => this.handleSelect(event)}
        />
      </div>
    );
  }

  renderHeaders() {
    let row = this.props.headers.map((h, i) => {
      return <th key={i}>{h}</th>;
    });
    return <tr>{row}</tr>;
  }

  render() {
    if (this.state.displayItems.length > 0) {
      return (
        <div className="table-container">
          <Table responsive={true}>
            <thead>
              {this.renderHeaders()}
            </thead>
            <tbody>
              {this.renderTableRows()}
            </tbody>
          </Table>
          {this.renderPagination()}
        </div>
      );
    }
    return (
      <div className="empty-table-message">
          {this.props.emptyMessage}
      </div>
    );
  }
}

SimpleTable.propTypes = {
  emptyMessage: React.PropTypes.string.isRequired,
  entries: React.PropTypes.array.isRequired,
  perPage: React.PropTypes.number,
  first: React.PropTypes.bool,
  last: React.PropTypes.bool,
  headers: React.PropTypes.array.isRequired,
  renderTableRow: React.PropTypes.func.isRequired,
  maxButtons: React.PropTypes.number
};
