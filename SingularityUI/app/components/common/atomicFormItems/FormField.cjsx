Utils = require '../../../utils'

FormField = React.createClass

    defaultSize: 50

    getSize: ->
        if @props.prop.size
            return @props.prop.size
        else
            return @defaultSize

    render: ->
        <input 
            className = 'form-control'
            placeholder = {@props.prop.placeholder}
            type = {@props.prop.inputType}
            id = {@props.id}
            onChange = {@props.prop.updateFn} 
            value = {@props.prop.value}
            disabled = {if @props.prop.disabled then true else false}
        />

module.exports = FormField
