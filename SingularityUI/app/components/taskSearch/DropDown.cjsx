Utils = require '../../utils'

DropDown = React.createClass

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
                    <th><select type={@props.inputType} onChange={@props.updateFn} value={@props.value} defaultValue={@props.defaultValue}>
                        {dropDownOpts}
                    </select></th>
                </tr>

module.exports = DropDown