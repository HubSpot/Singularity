Utils = require '../../utils'

DropDown = React.createClass

	getInitialState: ->
		if @props.forceChooseValue
			return {
				value: @props.defaultValue
			}
		else
			return {
				value: 'noValueChosen'
			}

	handleChange: (event) ->
		@setState {
			value: event.target.value
		}

	render: ->
        dropDownOpts = []
        if not @props.forceChooseValue
            dropDownOpts.push(<option key=0 value='noValueChosen'>Any</option>)
        i = 1
        for element in @props.choices
            dropDownOpts.push(<option key={i} value={element.value}>{element.user}</option>)
            i++
        return <tr>
                    <th><b> {@props.title} </b></th>
                    <th><select type={@props.inputType} onChange={@handleChange} value={@state.value} defaultValue={@props.defaultValue}>
                        {dropDownOpts}
                    </select></th>
                </tr>

module.exports = DropDown