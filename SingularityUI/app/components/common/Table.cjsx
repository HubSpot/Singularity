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
        elements.map (element, key) =>
            ComponentClass = element.component
            return <td key={key}>
                <ComponentClass
                    prop=element.prop
                />
            </td>

    renderTableData: ->
        @props.tableRows.map (tableRow, key) =>
            <tr key={key}>{@renderTableRow tableRow}</tr>


    # Note: Use @props.tableClassOpts to declare things like striped or bordered
    getClassName: ->
        return "table #{@props.tableClassOpts}"

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
