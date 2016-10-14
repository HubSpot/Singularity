import React, { Component, PropTypes } from 'react';
import Waypoint from 'react-waypoint';
import classNames from 'classnames';

import BootstrapTable from 'react-bootstrap/lib/Table';
import { Pagination } from 'react-bootstrap';

class UITable extends Component {

  constructor(props) {
    super(props);

    let { data } = props;
    const { defaultSortBy, defaultSortDirection, resultsPerPage } = props;
    if (defaultSortBy) {
      data = this.doSort(data, defaultSortBy, defaultSortDirection);
    }

    this.state = {
      sortBy: defaultSortBy,
      sortDirection: defaultSortDirection,
      sortTime: null,
      curPage: 1,
      data,
      resultsPerPage
    };

    this.handlePageChange = this.handlePageChange.bind(this);
  }

  componentWillReceiveProps(nextProps) {
    this.updateSort(nextProps.data, this.state.sortBy, this.state.sortDirection);
    if (nextProps.isFetching) {
      return;
    }
    if (this.isApiPaginated()) {
      if (nextProps.page == nextProps.maxPage) {
        this.setState({ lastPage : true, data : nextProps.data });
      } else {
        this.setState({ data : nextProps.data });
      }
    }
  }

  static SortDirection = {
    ASC: 'ASC',
    DESC: 'DESC'
  };

  static defaultProps = {
    paginated: false,
    resultsPerPage: 30,
    defaultSortBy: undefined,
    defaultSortDirection: UITable.SortDirection.DESC,
    asyncSort: false,
    maxPaginationButtons: 10
  };

  state;

  isApiPaginated() {
    return !!this.props.fetchDataFromApi;
  }

  
  resetPageAndChunkSizeWithoutChangingData(table) {
    return () => {
      table.setState({
        curPage: 1,
        resultsPerPage: this.props.resultsPerPage,
        lastPage: false,
        pastEnd: false
      });
    };
  }

  fetchDataFromApi(curPage, resultsPerPage, updateStateAfterFetching = false, sortBy = this.state.sortBy) {
    let lastPage = this.state.lastPage;
    if (curPage < this.state.curPage) {
      // going back a page
      lastPage = false;
    }
    if (!updateStateAfterFetching) {
      this.setState({curPage, resultsPerPage, sortBy, lastPage});
    }

    return this.props.fetchDataFromApi(curPage, resultsPerPage, sortBy).then(() => {
      if (updateStateAfterFetching) {
        this.setState({curPage, resultsPerPage, sortBy, lastPage});
      }
    });
  }

  updateSort(data, sortBy, sortDirection) {
    if (this.isApiPaginated()) {
      this.setState({
        sortBy,
        sortDirection,
        sortTime: Date.now()
      });
      return;
    }
    if (this.props.asyncSort) {
      const sortTimeStart = Date.now();
      this.setState({
        sortBy,
        sortDirection,
        sortTime: sortTimeStart,
        sortFinished: false
      });

      new Promise((resolve) => {
        const sorted = this.doSort(data, sortBy, sortDirection);
        resolve({sorted, sortTime: sortTimeStart});
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
    const numPages = Math.ceil(nextState.data.length / this.state.resultsPerPage);
    const updatedPage = this.state.curPage > numPages && numPages !== 0
      ? numPages
      : this.state.curPage;

    let updatedNextState = nextState;

    if (this.props.paginated) {
      updatedNextState = {
        ...updatedNextState,
        curPage: updatedPage
      };
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

    const { cellData, sortData } = sortCol.props;
    /* eslint-disable id-length */ // Exception for comparator
    const sorted = data.concat().sort((a, b) => {
      /* eslint-enable id-length */
      return sortCol.props.sortFunc(
        sortData(cellData(a), a),
        sortData(cellData(b), b)
      );
    });

    if (sortDirection === UITable.SortDirection.ASC) {
      sorted.reverse();
    }

    return sorted;
  }

  handlePageChange(eventKey) {
    if (eventKey === this.state.curPage) {
      return;
    }
    if (this.isApiPaginated()) {
      this.fetchDataFromApi(eventKey, this.state.resultsPerPage);
      return;
    }
    const page = eventKey;
    const numPages = Math.ceil(this.state.data.length / this.state.resultsPerPage);

    this.setState({
      curPage: Math.min(Math.max(1, page), numPages)
    });
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
    if (this.isApiPaginated()) {
      this.props.fetchDataFromApi(this.state.curPage, this.state.resultsPerPage, false, col.props.id);
    }
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

  shouldRenderPagination(numRows, rowsPerPage) {
    return this.isApiPaginated() || (this.props.paginated && numRows > rowsPerPage);
  }

  renderTableRow(rowData, index) {
    const row = this.props.children.map((col) => {
      const cellData = col.props.cellData(rowData);
      const cell = col.props.cellRender(
        cellData,
        rowData
      );

      let className;
      if (typeof col.props.className === 'function') {
        className = col.props.className(cellData, rowData);
      } else if (col.props.className) {
        className = col.props.className;
      }

      return <td key={col.props.id} className={className}>{cell}</td>;
    });
    let rowClassName;
    if (typeof this.props.rowClassName === 'function') {
      rowClassName = this.props.rowClassName(rowData, index);
    } else if (this.props.rowClassName) {
      rowClassName = this.props.rowClassName;
    }
    return <tr key={`row-${this.props.keyGetter(rowData)}`} className={rowClassName}>{row}</tr>;
  }

  renderTableRows() {
    if (this.props.paginated && !this.isApiPaginated()) {
      const page = this.state.curPage;
      const beginIndex = (page - 1) * this.state.resultsPerPage;
      const endIndex = page * this.state.resultsPerPage;
      const rows = this.state.data.slice(beginIndex, endIndex).map((row, index) => {
        return this.renderTableRow(row, index);
      });

      return rows;
    } else if (this.props.paginated) {
      return this.props.data.map((row, index) => {
        return this.renderTableRow(row, index);
      });
    }
    // infinite scrolling
    // Only render a number of rows at a time
    // check to see if we can render of everything
    const maxVisibleRows = this.props.renderAllRows ? this.state.data.length : this.state.curPage * this.state.resultsPerPage;
    const rows = this.state.data.slice(0, maxVisibleRows).map((row) => {
      return this.renderTableRow(row);
    });

    if (maxVisibleRows < this.state.data.length) {
      return [...rows, this.renderWaypoint()];
    }

    return rows;
  }

  renderRequestPerPageChoices() {
    const setRequestPerPage = (resultsPerPage) => {
      if (this.isApiPaginated()) {
        this.fetchDataFromApi(1, resultsPerPage, true);
      } else {
        this.setState({curPage: 1, resultsPerPage});
      }
    };

    return (
      <div className="pull-right count-options">
        Results per page:
        {this.props.requestPerPageChoices.map((choice) =>
          <a key={choice} className={classNames({inactive: choice === this.state.resultsPerPage})} onClick={() => setRequestPerPage(choice)}>
            {choice}
          </a>
        )}
      </div>
    );
  }

  renderPagination() {
    const numRows = this.state.data.length;
    const rowsPerPage = this.state.resultsPerPage;
    const maxPaginationButtons = this.isApiPaginated() ? 1 : this.props.maxPaginationButtons;
    if (this.shouldRenderPagination(numRows, rowsPerPage)) {
      let numPages = this.props.maxPage;
      
      return (
        <Pagination
          prev={true}
          next={true}
          first={numPages > maxPaginationButtons}
          last={!this.isApiPaginated() && numPages > maxPaginationButtons}
          ellipsis={false}
          items={numPages}
          maxButtons={maxPaginationButtons}
          activePage={this.state.curPage}
          onSelect={this.handlePageChange}
        />
      );
    }

    return undefined;
  }

  renderWaypoint() {
    return (
      <tr key="waypoint">
        <td colSpan={this.props.children.length}>
          Loading...
          <Waypoint
            scrollableAncestor={window}
            key={`waypoint${this.state.curPage}`}
            onEnter={() => {
              const maxVisibleRows = this.state.curPage * this.state.resultsPerPage;
              if (maxVisibleRows < this.state.data.length) {
                _.defer(() => {
                  this.setState({
                    curPage: this.state.curPage + 1
                  });
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
    const headerRow = this.props.children.map((col) => {
      let cell;
      if (typeof col.props.label === 'function') {
        cell = col.props.label(this.props.data);
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

      return <th key={col.props.id} onClick={maybeOnClick} className={headerClasses}>{cell}{sortIndicator}</th>;
    });


    return <tr>{headerRow}</tr>;
  }

  render() {
    if (this.props.showPageLoaderWhenFetching && this.props.isFetching) {
      return <div className="page-loader fixed" />;
    }
    let maybeTable = (
      <BootstrapTable responsive={true} striped={true} className={this.props.className}>
        <thead>
          {this.renderTableHeader()}
        </thead>
        <tbody>
          {this.renderTableRows()}
        </tbody>
      </BootstrapTable>
    );

    if (this.props.emptyTableMessage && !this.props.data.length) {
      maybeTable = (
        <div className="empty-table-message">
          {this.props.emptyTableMessage}
        </div>
      );
    }

    return (
      <div>
        {this.props.requestPerPageChoices && <div className="row"><div className="col-md-12">{this.renderRequestPerPageChoices()}</div></div>}
        {maybeTable}
        {this.renderPagination()}
      </div>
    );
  }
}

UITable.propTypes = {
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
  keyGetter: PropTypes.func.isRequired,
  children: PropTypes.arrayOf(PropTypes.node).isRequired,
  paginated: PropTypes.bool,
  renderAllRows: PropTypes.bool,
  resultsPerPage: PropTypes.number,
  resultsPerPageChoices: PropTypes.arrayOf(PropTypes.number),
  maxPaginationButtons: PropTypes.number,
  defaultSortBy: PropTypes.string,
  defaultSortDirection: PropTypes.oneOf([
    UITable.SortDirection.ASC,
    UITable.SortDirection.DESC
  ]),
  className: PropTypes.string,
  asyncSort: PropTypes.bool,
  fetchDataFromApi: PropTypes.func, // (page, numberPerPage, sortBy) -> Promise // Makes this table API-paginated - no sorting
  isFetching: PropTypes.bool,
  // For long API calls set this to true. As a future upgrade it would be nice to automatically detect if it's taking a long time:
  showPageLoaderWhenFetching: PropTypes.bool,
  rowClassName: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.func
  ]),
  emptyTableMessage: PropTypes.oneOfType([
    PropTypes.node,
    PropTypes.string
  ])
};

export default UITable;
