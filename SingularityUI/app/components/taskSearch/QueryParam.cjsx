QueryParam = React.createClass

    render: ->
        return <p>{@props.paramName}: {@props.paramValue}</p>

module.exports = QueryParam