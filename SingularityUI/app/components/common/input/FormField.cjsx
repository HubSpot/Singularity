Utils = require '../../../utils'

FormField = React.createClass

    defaultSize: 50

    getSize: ->
        if @props.size
            return @props.size
        else
            return @defaultSize

    getDisabled: ->
        if @props.disabled
            return true
        else
            return false

    render: ->
        <tr>
            <th><input 
                    className = 'form-control'
                    placeholder = {@props.title}
                    type = {@props.inputType} 
                    onChange = {@props.updateFn} 
                    value = {@props.value}
                    disabled = {@getDisabled()}
                    size = {@getSize()} />
            </th>
        </tr>

module.exports = FormField