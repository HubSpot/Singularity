Utils = require '../../../utils'

FormField = React.createClass

    defaultSize: 50

    getSize: ->
        if @props.size
            return @props.size
        else
            return @defaultSize

    render: ->
        <input 
            className = 'form-control'
            placeholder = {@props.title}
            type = {@props.inputType}
            id = {@props.id}
            onChange = {@props.updateFn} 
            value = {@props.value}
            disabled = {if @props.disabled then true else false}
        />

module.exports = FormField