import React from 'react';
import Link from './atomicDisplayItems/Link';
import Glyphicon from './atomicDisplayItems/Glyphicon';
import IconButton from './atomicDisplayItems/IconButton';
import Utils from '../../utils';

// This table won't handle paging or sorting for you,
// but allows you to designate columns as sortable
// with functions that trigger sorting by those columns
let Table = React.createClass({

    defaultSortDirectionAscending: true,

    propTypes: {
        columnHeads: React.PropTypes.arrayOf(React.PropTypes.shape({
            data: React.PropTypes.string,
            className: React.PropTypes.string,
            doSort: React.PropTypes.func,
            sortable: React.PropTypes.boolean,
            sortAttr: React.PropTypes.string
        })).isRequired,

        tableRows: React.PropTypes.arrayOf(React.PropTypes.shape({
            dataId: React.PropTypes.string.isRequired,
            className: React.PropTypes.string,
            data: React.PropTypes.arrayOf(React.PropTypes.shape({
                component: React.PropTypes.func.isRequired,
                prop: React.PropTypes.object,
                id: React.PropTypes.string,
                className: React.PropTypes.string
            })).isRequired
        })).isRequired,

        tableClassOpts: React.PropTypes.string,

        sortDirection: React.PropTypes.any,
        sortDirectionAscending: React.PropTypes.any,
        sortBy: React.PropTypes.string,
        customSorting: React.PropTypes.bool,

        emptyTableMessage: React.PropTypes.string,

        customPaging: React.PropTypes.bool,
        defaultRowsPerPage: React.PropTypes.number,
        rowsPerPageChoices: React.PropTypes.arrayOf(React.PropTypes.number),
        setRowsPerPage: React.PropTypes.func,
        pageNumber: React.PropTypes.number,
        pageDown: React.PropTypes.func,
        pageUp: React.PropTypes.func,
        noPages: React.PropTypes.bool,

        dataCollection: React.PropTypes.string
    },

    defaultRowsPerPageChoices: [5, 10, 15, 20],

    getInitialState() {
        return {
            rowsPerPage: this.props.defaultRowsPerPage ? this.props.defaultRowsPerPage : 5,
            pageNumber: 1
        };
    },

    rowsPerPage() {
        if (this.props.rowsPerPage) {
            return this.props.rowsPerPage;
        } else {
            return this.state.rowsPerPage;
        }
    },

    setRowsPerPage(rows) {
        if (this.props.customPaging) {
            return this.props.setRowsPerPage(rows);
        }
        return this.setState({ rowsPerPage: rows });
    },

    rowsPerPageChoices() {
        if (this.props.rowsPerPageChoices) {
            return this.props.rowsPerPageChoices;
        } else {
            return this.defaultRowsPerPageChoices;
        }
    },

    renderRowsPerPageChoices() {
        if (this.props.noPages) {
            return null;
        }
        let choices = [];
        this.rowsPerPageChoices().map(choice => {
            return choices.push(<Link key={choice} prop={{
                url: '#',
                title: `${ choice } rows per page`,
                text: choice,
                className: 'half-roomy-right',
                onClickFn: () => this.setRowsPerPage(choice)
            }} />);
        });
        return <div title='Rows Per Page' className='pull-right'>Results Per Page:{choices}</div>;
    },

    clearSort() {
        if (this.props.customSorting) {
            return;
        }
        return this.setState({
            sortBy: undefined,
            sortDirection: undefined
        });
    },

    sortDirection() {
        if (this.props.customSorting) {
            return this.props.sortDirection;
        } else {
            return this.state.sortDirectionAscending;
        }
    },

    sortBy() {
        if (this.props.customSorting) {
            return this.props.sortBy;
        } else {
            return this.state.sortBy;
        }
    },

    sortDirectionAscending() {
        if (this.props.customSorting) {
            return this.props.sortDirectionAscending;
        } else {
            return true;
        }
    },

    makeColumnHeadSortFn(columnHead) {
        if (this.props.customSorting) {
            return columnHead.doSort;
        } else {
            return () => {
                if (this.state.sortBy === columnHead.data) {
                    var newSortDirectionAscending = !this.state.sortDirectionAscending;
                } else {
                    var newSortDirectionAscending = this.defaultSortDirectionAscending;
                }
                this.setState({
                    sortDirectionAscending: newSortDirectionAscending,
                    sortBy: columnHead.data
                });
                columnHead.doSort(newSortDirectionAscending);
                return this.forceUpdate();
            };
        }
    },

    getSortableColumnHeadGlyphicon(columnHead) {
        if (this.sortBy() !== columnHead.data) {
            return;
        }
        if (this.sortDirection() === this.sortDirectionAscending()) {
            return <Glyphicon iconClass='chevron-up' />;
        } else {
            return <Glyphicon iconClass='chevron-down' />;
        }
    },

    pageNumber() {
        if (this.props.customPaging) {
            return this.props.pageNumber;
        } else {
            return this.state.pageNumber;
        }
    },

    pageUpDisabled() {
        return this.props.customPaging && (this.props.isLastPage || this.props.tableRows.length < this.rowsPerPage()) || this.state.pageNumber * this.state.rowsPerPage >= this.props.tableRows.length && !this.props.customPaging;
    },

    pageDown() {
        if (this.pageNumber() === 1) {
            return;
        }
        if (this.props.customPaging) {
            return this.props.pageDown();
        }
        if (this.state.pageNumber > 1) {
            return this.setState({ pageNumber: this.state.pageNumber - 1 });
        }
    },

    pageUp() {
        if (this.pageUpDisabled()) {
            return;
        }
        if (this.props.customPaging) {
            return this.props.pageUp();
        }
        if (!this.pageUpDisabled()) {
            return this.setState({ pageNumber: this.state.pageNumber + 1 });
        }
    },

    renderPageButtons() {
        if (this.props.noPages) {
            return null;
        }
        return <div><div className='col-xs-5' /><div className='col-xs-1'><IconButton prop={{
                    iconClass: 'chevron-left',
                    btnClass: 'default',
                    ariaLabel: 'pageDown',
                    alt: 'pageDown',
                    className: {
                        'col-xs-5': true,
                        'hide': this.pageNumber() === 1
                    },
                    onClick: this.pageDown
                }} /></div><div className='col-xs-1'><IconButton prop={{
                    iconClass: 'chevron-right',
                    btnClass: 'default',
                    ariaLabel: 'pageUp',
                    alt: 'pageUp',
                    className: {
                        'col-xs-5': true,
                        'hide': this.pageUpDisabled()
                    },
                    onClick: this.pageUp
                }} /></div><div className='col-xs-5' /></div>;
    },

    /* CORE FUNCTIONALITY */

    /* 
    NOTE: columnHead.doSort() should do at least three things:
        - explicitly set @props.sortDirection
        - explicitly set @props.sortBy
        - sort @props.tableRows
    */
    getColumnHeadData(columnHead) {
        if (!columnHead.sortable) {
            return columnHead.data;
        }
        return <Link prop={{
            url: '#',
            title: `Sort By ${ columnHead.data }`,
            onClickFn: event => {
                event.preventDefault();
                let sort = this.makeColumnHeadSortFn(columnHead);
                return sort();
            },
            text: <div>{columnHead.data} {this.getSortableColumnHeadGlyphicon(columnHead)}</div>
        }} />;
    },

    renderTableHeader() {
        return this.props.columnHeads.map((columnHead, key) => {
            return <th key={key} className={columnHead.className}>{this.getColumnHeadData(columnHead)}</th>;
        });
    },

    renderTableRow(elements) {
        return elements.map((element, key) => {
            if (typeof element === 'object') {
                let ComponentClass = element.component;
                return <td key={key} className={element.className}><ComponentClass prop={element.prop} /></td>;
            } else {
                return <td key={key}>{element}</td>;
            }
        });
    },

    renderEmptyTable() {
        return <div className="empty-table-message">{this.props.emptyTableMessage}</div>;
    },

    displayThisRow(rowNr) {
        if (this.props.customPaging || this.props.noPages) {
            return true;
        }
        let minRow = (this.state.pageNumber - 1) * this.rowsPerPage();
        let maxRow = this.state.pageNumber * this.rowsPerPage() - 1;
        return minRow <= rowNr && rowNr <= maxRow;
    },

    renderTableData() {
        return this.props.tableRows.map((tableRow, key) => {
            if (!this.displayThisRow(key)) {
                return;
            }
            return <tr key={key} data-id={tableRow.dataId} data-collection={tableRow.dataCollection}>{this.renderTableRow(tableRow.data)}</tr>;
        });
    },

    /* 
        - Use @props.tableClassOpts to declare things like striped or bordered
        - Use @props.customSorting if the API for models this table will display
          keeps track of sort direction on its own and @props.customPaging if it'll page them on its own
        - @props.customSorting indicates that you will be providing your own functions to sort the table rows
            - If provided, you must provide @props.sortBy, @props.sortDirection, @props.sortDirectionAscending,
              for each column you mark as sortable.
            - Either way you must provide a doSort function for each column marked as sortable. However,
              if you are customSorting the function will take no arguments. Otherwise it will take a boolean
              true if sort direction is ascending, false if descending.
              YOUR doSort FUNCTION MAY HAVE TO CALL forceUpdate() TO BE ABLE TO SEE THE SORTED COLLECTION
        - @props.customPaging indicates that you will be providing your own functions to handle table pages
            - If provided, you must provide @props.setRowsPerPage, @props.increasePage, @props.decreasePage, @props.pageNumber
        - @props.noPages indicates that the table will display every element on one page, regardless of how many elements there are
    */
    getClassName() {
        return `table ${ this.props.tableClassOpts }`;
    },

    render() {
        if (this.props.tableRows.length === 0 && this.pageNumber() === 1) {
            return this.renderEmptyTable();
        }
        return <div>{this.renderRowsPerPageChoices()}<div className='table-container'><table className={this.getClassName()}><thead><tr>{this.renderTableHeader()}</tr></thead><tbody>{this.renderTableData()}</tbody></table></div>{this.renderPageButtons()}</div>;
    }
});

export default Table;

