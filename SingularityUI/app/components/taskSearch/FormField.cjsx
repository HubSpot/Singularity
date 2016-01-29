Utils = require '../../utils'

FormField = React.createClass

	getInitialState: ->
		return {
			value: @props.initialValue
		}

	handleChange: (event) ->
		@setState {
			value: event.target.value
		}

	render: ->
		<tr>
            <th><b> {@props.title} </b> </th>
            <th><input 
            		type = {@props.inputType} 
            		onChange = {@handleChange} 
            		value = {@state.value} 
            		size = 50 />
            </th>
        </tr>

module.exports = FormField