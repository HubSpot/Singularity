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

  updateDisplay(nextProps) {
    this.setState({
      displayItems: nextProps.entries
    });
  }

  componentWillReceiveProps(nextProps) {
    this.updateDisplay(nextProps);
  }

  handleSelect(event, selectedEvent) {
    this.setState({
      activePage: selectedEvent.eventKey
    });
  }

  renderTableRows() {
    let page = this.state.activePage - 1;
    let items = this.props.perPage;
    let entries = this.state.displayItems.slice(page * items, (page * items) + items);
    const rows = entries.map((e, i) => {
      return this.props.renderTableRow(e, i);
    });
    return rows;
  }

  renderPagination() {
    if (this.state.displayItems.length > this.props.perPage) {
      return (
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
            onSelect={this.handleSelect.bind(this)} />
        </div>
      );
    }
  }

  render() {
    if (this.state.displayItems.length > 0) {
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
            {this.props.emptyMessage}
        </div>
      );
    }
  }
}

SimpleTable.propTypes = {
  emptyMessage: React.PropTypes.string.isRequired,
  entries: React.PropTypes.array.isRequired,
  perPage: React.PropTypes.number,
  first: React.PropTypes.bool,
  last: React.PropTypes.bool,
  renderTableHeaders: React.PropTypes.func.isRequired,
  renderTableRow: React.PropTypes.func.isRequired
};
