React = require 'react'
IconButton = require './IconButton'

QueryParameter = React.createClass

    getClassName: ->
        classNames {
            "list-group-item": true
            "disabled": @props.prop.cantClear
        }

    render: ->
        return  <li className={classNames {
                                "list-group-item": true
                                "disabled": @props.prop.cantClear
                    }}>
                        <b>{@props.prop.paramName}:</b> {@props.prop.paramValue} 
                        {<IconButton 
                            prop = {{
                                ariaLabel: 'Remove this parameter'
                                iconClass: 'remove'
                                className: ['remove-query-param', 'pull-right']
                                btnClass:'default'
                                btn: false
                                onClick: @props.prop.clearFn
                            }}
                        /> unless @props.prop.cantClear}
                </li>
module.exports = QueryParameter
