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
            disabled = {if @props.prop.disabled then true else false}
            min = @props.prop.min
            max = @props.prop.max
            required = {if @props.prop.required then true else false} 
        />

module.exports = FormField
