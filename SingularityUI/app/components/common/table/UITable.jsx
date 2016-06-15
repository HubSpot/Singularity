import React, { Component, PropTypes } from 'react';

import BootstrapTable from 'react-bootstrap/lib/Table';

class UITable extends Component {
  static SortDirection = {
    ASC: 'ASC',
    DESC: 'DESC'
  };

  static propTypes = {
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    keyGetter: PropTypes.func.isRequired,
    rowChunkSize: PropTypes.number,
    defaultSortBy: PropTypes.string,
    defaultSortDirection: PropTypes.oneOf([
      UITable.SortDirection.ASC,
      UITable.SortDirection.DESC
    ]),
    className: PropTypes.string
  };

  
  static defaultProps = {
    rowChunkSize: 10,
    defaultSortBy: null,
    defaultSortDirection: UITable.SortDirection.ASC
  };

  state;

  constructor(props) {
    super(props);
    this.displayName = 'UITable';

    this.state = {
      sortBy: props.defaultSortBy,
      sortDirection: props.defaultSortDirection,
      chunkNum: 1
    }
  }

  renderTableRow(rowData) {
    const row = this.props.children.map((col, tdIndex) => {
      const cell = col.props.renderCell(
        col.props.cellData(rowData)
      );

      return <td key={tdIndex} className={col.props.className}>{cell}</td>;
    });
    return <tr key={this.props.keyGetter(rowData)}>{row}</tr>;
  }

  renderTableRows() {
    // Only render a number of rows at a time
    // check to see if we can render of everything
    const maxVisibleRows = this.state.chunkNum * this.props.rowChunkSize;
    if (maxVisibleRows >= this.props.data.length) {

    }

    return this.props.data.map((r, index) => {
      return this.renderTableRow(r, index);
    });
  }

  renderExtraRowsWatcher() {

  }

  renderTableHeader() {
    const headerRow = this.props.children.map((col, thIndex) => {
      let cell;
      if (typeof col.props.label === 'function') {
        cell = col.props.label(
          col.props.cellData(rowData)
        );
      } else {
        // should only be string according to propTypes
        cell = col.props.label;
      }

      return <th key={thIndex} className={col.props.headerClassName || ''}>{cell}</th>;
    });
    return <tr>{headerRow}</tr>;
  }

  render() {
    return (
      <BootstrapTable responsive className={this.props.className}>
        <thead>
          {this.renderTableHeader()}
        </thead>
        <tbody>
          {this.renderTableRows()}
        </tbody>
      </BootstrapTable>
    );
  }
}

export default UITable;
