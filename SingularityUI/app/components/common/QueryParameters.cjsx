React = require 'react'
QueryParameter = require "./atomicDisplayItems/QueryParameter"

QueryParameters = React.createClass

    renderParameters: ->
        @props.parameters.map (parameter, key) =>
            if parameter.show
                <div key={key}> 
                    <QueryParameter
                        paramName = parameter.name
                        paramValue = parameter.value
                        clearFn = parameter.clearFn
                        cantClear = parameter.cantClear
                    />
                </div>

    render: ->
        <div className="row">
            <div className="col-#{@props.colSize}">
                <ul className="list-group">
                    {@renderParameters()}
                </ul>
            </div>
        </div>
        

module.exports = QueryParameters
