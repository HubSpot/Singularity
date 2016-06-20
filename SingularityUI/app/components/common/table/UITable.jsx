import React, { Component, PropTypes } from 'react';
import Waypoint from 'react-waypoint';
import classNames from 'classnames';
import _ from 'underscore';

import BootstrapTable from 'react-bootstrap/lib/Table';
import { Pagination } from 'react-bootstrap';

class UITable extends Component {
  static SortDirection = {
    ASC: 'ASC',
    DESC: 'DESC'
  };

  static propTypes = {
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    keyGetter: PropTypes.func.isRequired,
    paginated: PropTypes.bool,
    rowChunkSize: PropTypes.number,
    sortBy: PropTypes.string,
    sortDirection: PropTypes.oneOf([
      UITable.SortDirection.ASC,
      UITable.SortDirection.DESC
    ]),
    className: PropTypes.string,
    asyncSort: PropTypes.bool
  };

  
  static defaultProps = {
    paginated: false,
    rowChunkSize: 30,
    sortBy: undefined,
    sortDirection: UITable.SortDirection.DESC,
    asyncSort: false
  };

  state;

  constructor(props) {
    super(props);
    this.displayName = 'UITable';

    this.state = {
      sortBy: props.defaultSortBy,
      sortDirection: props.defaultSortDirection,
      sortTime: null,
      chunkNum: 1,
      data: props.data
    }
  }

  componentWillReceiveProps(nextProps) {
    this.updateSort(nextProps.data, this.state.sortBy, this.state.sortDirection);
  }

  updateSort(data, sortBy, sortDirection) {
    if (this.props.asyncSort) {
      const sortTime = Date.now();
      this.setState({
        sortBy,
        sortDirection,
        sortTime,
        sortFinished: false
      });

      new Promise((resolve, reject) => {
        const sorted = this.doSort(data, sortBy, sortDirection);
        resolve({sorted, sortTime});
      }).then(({sorted, sortTime}) => {
        if (this.state.sortTime === sortTime) {
          // same sort that was last triggered, let's update the state
          this.refitPagination({
            data: sorted,
            sortFinished: true
          });
        } else {
          // Abort: another sort finished faster
        }
      });
    } else {
      this.refitPagination({
        sortBy,
        sortDirection,
        data: this.doSort(data, sortBy, sortDirection)
      });
    }
  }

  refitPagination(nextState) {
    if (!('data' in nextState)) {
      this.setState(nextState);
    }

    // we have to update pagination if the new list size doesn't
    // have enough pages for the current page
    const numPages = (nextState.data.length / this.props.rowChunkSize);
    const updatedPage = this.state.chunkNum > numPages
      ? numPages
      : this.state.chunkNum;

    let updatedNextState = nextState;

    if (this.props.pagination) {
      updatedNextState = {
        ...updatedNextState,
        chunkNum: updatedPage
      }
    }

    this.setState(updatedNextState);
  }

  doSort(data, sortBy, sortDirection) {
    const sortCol = this.props.children.find((col) => {
      return col.props.id === sortBy;
    });
    if (sortCol === undefined) {
      return data;
    }

    const sorted = data.concat().sort((a, b) => {
      return sortCol.props.sortFunc(
        sortCol.props.cellData(a),
        sortCol.props.cellData(b)
      );
    });

    if (sortDirection === UITable.SortDirection.ASC) {
      sorted.reverse();
    }

    return sorted;
  }

  renderTableRow(rowData) {
    const row = this.props.children.map((col, tdIndex) => {
      const cell = col.props.cellRender(
        col.props.cellData(rowData)
      );

      return <td key={tdIndex} className={col.props.className}>{cell}</td>;
    });
    return <tr key={`row-${this.props.keyGetter(rowData)}`}>{row}</tr>;
  }

  renderTableRows() {
    if (this.props.paginated) {
      const page = this.state.chunkNum;
      const beginIndex = (page - 1) * this.props.rowChunkSize;
      const endIndex = page * this.props.rowChunkSize;
      const rows = this.state.data.slice(beginIndex, endIndex).map((r) => {
        return this.renderTableRow(r);
      });

      return rows;
    } else {
      // infinite scrolling
      // Only render a number of rows at a time
      // check to see if we can render of everything
      const maxVisibleRows = this.state.chunkNum * this.props.rowChunkSize;
      const rows = this.state.data.slice(0, maxVisibleRows).map((r) => {
        return this.renderTableRow(r);
      });

      if (maxVisibleRows < this.state.data.length) {
        return [...rows, this.renderWaypoint()];
      } else {
        return rows;
      }
    }
  }

  renderPagination() {
    const numRows = this.state.data.length;
    const rowsPerPage = this.props.rowChunkSize;
    if (this.props.paginated && numRows > rowsPerPage) {
      const numPages = Math.ceil(numRows / rowsPerPage);
      return (
        <Pagination
          prev
          next
          first
          last
          ellipsis={false}
          items={numPages}
          maxButtons={this.props.maxButtons || 10}
          activePage={this.state.chunkNum}
          onSelect={this.handlePageChange.bind(this)} />
      );
    }
  }

  handlePageChange(event, selectedEvent) {
    const page = selectedEvent.eventKey;
    const numPages = Math.ceil(this.state.data.length / this.props.rowChunkSize);

    this.setState({
      chunkNum: Math.min(Math.max(1, page), numPages)
    });
  }

  renderWaypoint() {
    return (
      <tr key={'waypoint'}>
        <td colspan={this.props.children.length}>
          <Waypoint
            key={'waypoint'}
            onEnter={() => {
              const maxVisibleRows = this.state.chunkNum * this.props.rowChunkSize;
              if (maxVisibleRows < this.state.data.length) {
                this.setState({
                  chunkNum: this.state.chunkNum + 1
                });
              }
            }}
            threshold={1}
          />
        </td>
      </tr>
    );
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

      let maybeOnClick;

      if (col.props.sortable) {
        maybeOnClick = () => this.handleSortClick(col);
      }

      const thisColumnSorted = col.props.id === this.state.sortBy;

      const headerClasses = classNames({
        'sortable': col.props.sortable,
        'column-sorted': thisColumnSorted,
        'column-sorted-asc': this.state.sortDirection === UITable.SortDirection.ASC && thisColumnSorted,
        'column-sorted-desc': this.state.sortDirection === UITable.SortDirection.DESC && thisColumnSorted
      }, col.props.headerClassName);

      const sortIndicator = this.sortIndicator(
        col.props.sortable,
        thisColumnSorted,
        this.state.sortDirection
      );

      return <th key={thIndex} onClick={maybeOnClick} className={headerClasses}>{cell}{sortIndicator}</th>;
    });


    return <tr>{headerRow}</tr>;
  }

  sortIndicator(sortable, thisColumnSorted, sortDirection) {
    if (sortable) {
      let classes = classNames({
        'glyphicon': thisColumnSorted,
        'glyphicon-triangle-bottom': thisColumnSorted && sortDirection === UITable.SortDirection.DESC,
        'glyphicon-triangle-top': thisColumnSorted && sortDirection === UITable.SortDirection.ASC,
        'pull-right': thisColumnSorted
      });

      return <span className={classes} />;
    }

    return undefined;
  }

  handleSortClick(col) {
    const colId = col.props.id;
    if (colId === this.state.sortBy) {
      // swap sort direction
      let newSortDirection;
      if (this.state.sortDirection === UITable.SortDirection.ASC) {
         newSortDirection = UITable.SortDirection.DESC;
      } else {
         newSortDirection = UITable.SortDirection.ASC;
      }

      this.updateSort(
        this.props.data,
        colId,
        newSortDirection
      );
    } else {
      this.updateSort(
        this.props.data,
        colId,
        UITable.SortDirection.DESC
      );
    }

    return true;
  }

  render() {
    return (
      <div>
        <BootstrapTable responsive className={this.props.className}>
          <thead>
            {this.renderTableHeader()}
          </thead>
          <tbody>
            {this.renderTableRows()}
          </tbody>
        </BootstrapTable>
        {this.renderPagination()}
      </div>
    );
  }
}

export default UITable;
