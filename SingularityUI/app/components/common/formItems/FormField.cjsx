React = require 'react'
classNames = require 'classnames'
Utils = require '../../../utils'

FormField = React.createClass

    render: ->
        <input 
            className = {classNames 'form-control', @props.prop.customClass}
            placeholder = {@props.prop.placeholder}
            type = {@props.prop.inputType}
            id = {@props.id}
            onChange = {@props.prop.updateFn} 
            value = {@props.prop.value}
            disabled = @props.prop.disabled
            min = @props.prop.min
            max = @props.prop.max
            required = @props.prop.required
        />

module.exports = FormField
