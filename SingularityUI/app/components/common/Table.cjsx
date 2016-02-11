Utils = require '../../utils'

Table = React.createClass

    renderTableHeader: ->
        header = []
        key = 0
        for columnName in @props.columnNames
            header.push(<th key={key}>{columnName}</th>)
            key++
        return header

    renderTableRow: (elements) ->
        data = []
        key = 0
        for element in elements
            data.push(<td key={key}>
                <element.component
                    prop=element.prop
                />
            </td>)
            key++
        return data

    # Note: The data in @props.tableRows should be the JSX you would like to render
    renderTableData: ->
        tableRows = []
        key = 0
        for tableRow in @props.tableRows
            tableRows.push(<tr key={key}>{@renderTableRow tableRow}</tr>)
            key++
        return tableRows


    # Note: Use @props.tableClassOpts to declare things like striped or bordered
    getClassName: ->
        return "table " + @props.tableClassOpts

    render: ->
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

module.exports = Table
