Utils = require '../../../utils'

FormField = React.createClass

    render: ->
        <tr>
            <th><input 
                    className = 'form-control'
                    placeholder = {@props.title}
                    type = {@props.inputType} 
                    onChange = {@props.updateFn} 
                    value = {@props.value} 
                    size = 50 />
            </th>
        </tr>

module.exports = FormField