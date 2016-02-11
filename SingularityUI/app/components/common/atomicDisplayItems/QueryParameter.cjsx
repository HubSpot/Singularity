IconButton = require '../IconButton'

QueryParameter = React.createClass

    getClassName: ->
        classNames {
            "list-group-item": true
            "disabled": @props.cantClear
        }

    render: ->
        return  <li className={classNames {
                                "list-group-item": true
                                "disabled": @props.cantClear
                    }}>
                        <b>{@props.paramName}:</b> {@props.paramValue} 
                        {<IconButton 
                            ariaLabel='Remove this parameter'
                            className={['remove-query-param', 'pull-right', 'glyphicon-remove']} 
                            onClick={@props.clearFn} 
                        /> unless @props.cantClear}
                </li>
module.exports = QueryParameter
