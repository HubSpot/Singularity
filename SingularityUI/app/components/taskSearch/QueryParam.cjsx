QueryParam = React.createClass

    getClassName: ->
        name="list-group-item"
        if @props.cantClear
            name += " disabled"
        return name

    render: ->
        return  <li className={@getClassName()}>
                        <b>{@props.paramName}:</b> {@props.paramValue} 
                        {<span className="remove-query-param glyphicon glyphicon-remove pull-right" onClick={@props.onClick} /> unless @props.cantClear}
                </li>
module.exports = QueryParam