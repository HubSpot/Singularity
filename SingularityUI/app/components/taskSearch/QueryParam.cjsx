IconButton = require '../common/IconButton'

QueryParam = React.createClass

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
                            className={['remove-query-param', 'pull-right', 'glyphicon-remove']} 
                            onClick={@props.onClick} 
                        /> unless @props.cantClear}
                </li>
module.exports = QueryParam
