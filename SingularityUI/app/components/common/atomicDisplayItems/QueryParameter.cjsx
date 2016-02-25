React = require 'react'
IconButton = require './IconButton'

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
                            iconClass='remove'
                            className={['remove-query-param', 'pull-right']}
                            btnClass='default'
                            btn = false
                            onClick={@props.clearFn}
                        /> unless @props.cantClear}
                </li>
module.exports = QueryParameter
