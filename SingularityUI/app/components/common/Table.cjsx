React = require 'react'
Link = require './atomicDisplayItems/Link'
Glyphicon = require './atomicDisplayItems/Glyphicon'
IconButton = require './atomicDisplayItems/IconButton'
Utils = require '../../utils'

# This table won't handle paging or sorting for you, 
# but allows you to designate columns as sortable 
# with functions that trigger sorting by those columns
Table = React.createClass

    propTypes:
        columnHeads: React.PropTypes.arrayOf(React.PropTypes.shape({
            data: React.PropTypes.string
            className: React.PropTypes.string
            doSort: React.PropTypes.func
            sortable: React.PropTypes.boolean
            sortAttr: React.PropTypes.string
        })).isRequired

        tableRows: React.PropTypes.arrayOf(React.PropTypes.shape({
            dataId: React.PropTypes.string.isRequired
            className: React.PropTypes.string
            data: React.PropTypes.arrayOf(React.PropTypes.shape({
                component: React.PropTypes.func.isRequired
                prop: React.PropTypes.object
                id: React.PropTypes.string
                className: React.PropTypes.string
            })).isRequired
        })).isRequired

        tableClassOpts: React.PropTypes.string

        sortDirection: React.PropTypes.any
        sortDirectionAscending: React.PropTypes.any
        sortBy: React.PropTypes.string
        customSorting: React.PropTypes.bool

        emptyTableMessage: React.PropTypes.string

        customPaging: React.PropTypes.bool
        defaultRowsPerPage: React.PropTypes.number
        rowsPerPageChoices: React.PropTypes.arrayOf(React.PropTypes.number)
        setRowsPerPage: React.PropTypes.func
        pageNumber: React.PropTypes.number
        pageDown: React.PropTypes.func
        pageUp: React.PropTypes.func

        dataCollection: React.PropTypes.string

    defaultRowsPerPageChoices: [5, 10, 15, 20]

    getInitialState: ->
        {
            rowsPerPage: if @props.defaultRowsPerPage then @props.defaultRowsPerPage else 5
            pageNumber: 1
        }

    rowsPerPage: ->
        if @props.rowsPerPage then @props.rowsPerPage else @state.rowsPerPage

    setRowsPerPage: (rows) ->
        return @props.setRowsPerPage rows if @props.customPaging
        @setState
            rowsPerPage: rows

    rowsPerPageChoices: ->
        if @props.rowsPerPageChoices then @props.rowsPerPageChoices else @defaultRowsPerPageChoices

    renderRowsPerPageChoices: ->
        choices = []
        @rowsPerPageChoices().map (choice) =>
            choices.push <Link
                    key = choice
                    prop = {{
                        url: '#'
                        title: "#{choice} rows per page"
                        text: choice
                        className: 'half-roomy-right'
                        onClickFn: () => @setRowsPerPage choice
                    }}
                />
        <div title='Rows Per Page' className='pull-right'>
            Results Per Page: {choices}
        </div>

    sortDirection: ->
        if @props.customSorting then @props.sortDirection else @state.SortDirection

    sortBy: ->
        if @props.customSorting then @props.sortBy else @state.SortBy

    sortDirectionAscending: ->
        if @props.customSorting then @props.sortDirectionAscending else 'ASC'

    makeColumnHeadSortFn: (columnHead) ->
        if @props.customSorting then columnHead.doSort else () ->
            # TODO

    getSortableColumnHeadGlyphicon: (columnHead) ->
        return unless @sortBy() is columnHead.data
        if @sortDirection() is @sortDirectionAscending()
            <Glyphicon iconClass='chevron-up' />
        else
            <Glyphicon iconClass='chevron-down' />

    pageNumber: ->
        if @props.customPaging then @props.pageNumber else @state.pageNumber

    pageUpDisabled: ->
        return (@props.customPaging and (@props.isLastPage or @props.tableRows.length < @rowsPerPage())) or 
            (@state.pageNumber * @state.rowsPerPage >= @props.tableRows.length and not @props.customPaging)

    pageDown: ->
        return if @pageNumber() is 1
        return @props.pageDown() if @props.customPaging
        @setState {pageNumber: @state.pageNumber - 1} if @state.pageNumber > 1

    pageUp: ->
        return if @pageUpDisabled()
        return @props.pageUp() if @props.customPaging
        @setState {pageNumber: @state.pageNumber + 1} unless @pageUpDisabled()

    renderPageButtons: ->
        <div>
            <div className = 'col-xs-5' />
            <div className = 'col-xs-1'>
                <IconButton
                    prop = {{
                        iconClass: 'chevron-left'
                        btnClass: 'default'
                        ariaLabel: 'pageDown'
                        alt: 'pageDown'
                        className: {
                            'col-xs-5': true
                            'hide': @pageNumber() is 1
                        }
                        onClick: @pageDown
                    }}
                />
            </div>
            <div className = 'col-xs-1'>
                <IconButton
                    prop = {{
                        iconClass: 'chevron-right'
                        btnClass: 'default'
                        ariaLabel: 'pageUp'
                        alt: 'pageUp'
                        className: {
                            'col-xs-5': true
                            'hide': @pageUpDisabled()
                        }
                        onClick: @pageUp
                    }}
                />
            </div>
            <div className = 'col-xs-5' />
        </div>

    ### CORE FUNCTIONALITY ###

    ### 
    NOTE: columnHead.doSort() should do at least three things:
        - explicitly set @props.sortDirection
        - explicitly set @props.sortBy
        - sort @props.tableRows
    ###
    getColumnHeadData: (columnHead) ->
        return columnHead.data unless columnHead.sortable
        <Link
            prop = {{
                url: '#'
                title: "Sort By #{columnHead.data}"
                onClickFn: @makeColumnHeadSortFn columnHead
                text: <div>{columnHead.data} {@getSortableColumnHeadGlyphicon columnHead}</div>
            }}
        />

    renderTableHeader: ->
        @props.columnHeads.map (columnHead, key) =>
            <th key={key} className={columnHead.className}>{@getColumnHeadData columnHead}</th>

    renderTableRow: (elements) ->
        elements.map (element, key) =>
            if typeof element is 'object'
                ComponentClass = element.component
                return <td key={key} className={element.className}>
                    <ComponentClass
                        prop=element.prop
                    />
                </td>
            else
                return <td key={key}>
                    {element}
                </td>

    renderEmptyTable: ->
        <div className="empty-table-message">
            {@props.emptyTableMessage}
        </div>

    displayThisRow: (rowNr) ->
        return true if @props.customPaging
        minRow = (@state.pageNumber - 1) * @rowsPerPage()
        maxRow = (@state.pageNumber * @rowsPerPage()) - 1
        return minRow <= rowNr <= maxRow

    renderTableData: ->
        @props.tableRows.map (tableRow, key) =>
            return unless @displayThisRow key
            <tr key={key} data-id={tableRow.dataId} data-collection={tableRow.dataCollection}>{@renderTableRow tableRow.data}</tr>


    ### 
        - Use @props.tableClassOpts to declare things like striped or bordered
        - Use @props.customSorting if the API for models this table will display
          sorts the models on its own and @props.customPaging if it'll page them on its own
        - @props.customSorting indicates that you will be providing your own functions to sort the table rows
            - If provided, you must provide @props.sortBy, @props.sortDirection, @props.sortDirectionAscending,
              and a doSort function for each column you mark as sortable.
        - @props.customPaging indicates that you will be providing your own functions to handle table pages
            - If provided, you must provide @props.setRowsPerPage, @props.increasePage, @props.decreasePage, @props.pageNumber
    ###
    getClassName: ->
        return "table #{@props.tableClassOpts}"

    render: ->
        return @renderEmptyTable() if @props.tableRows.length is 0 and @pageNumber() is 1
        <div>
            {@renderRowsPerPageChoices()}
            <div className='table-container'>
                <table className={@getClassName()}>
                    <thead>
                        <tr>
                            {@renderTableHeader()}
                        </tr>
                    </thead>
                    <tbody>
                        {@renderTableData()}
                    </tbody>
                </table>
            </div>
            {@renderPageButtons()}
        </div>

module.exports = Table
