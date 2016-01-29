Utils = require '../../utils'

FormField = React.createClass

	render: ->
		<tr>
            <th><b> {@props.title} </b> </th>
            <th><input 
            		type = {@props.inputType} 
            		onChange = {@props.updateFn} 
            		value = {@props.value} 
            		size = 50 />
            </th>
        </tr>

module.exports = FormField