QueryParam = React.createClass

    getInitialState: ->
        return {
            className: "collapse"
        }

    toggleClassName: ->
        return if @props.cantClear
        if @state.className == "collapse in"
            @setState({
                className: "collapse"
            })
        else
            @setState({
                className: "collapse in"
            })

    getButtonClassName: ->
        if @props.cantClear
            return "btn btn-primary disabled"
        else
            return "btn btn-primary"

    #Rolling my own (probably shoddy) collapse because the built-in one doesn't work well with dynamic ids
    render: ->
        return  <div>
                    <button className={@getButtonClassName()} onClick={@toggleClassName}>{@props.paramName}: {@props.paramValue}</button>
                    <div id={@props.paramName} className=@state.className >
                        <button className="btn btn-danger" onClick={@props.onClick}>Clear {@props.paramName}</button>
                    </div>
                </div>

module.exports = QueryParam