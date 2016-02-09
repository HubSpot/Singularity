Utils = require '../../../utils'

DropDown = React.createClass

    render: ->
        dropDownOpts = []
        dropDownOpts.push(<option key=0 value=''></option>)
        i = 1
        for element in @props.choices
            dropDownOpts.push(<option key={i} value={element.value}>{element.user}</option>)
            i++
        return <select className='form-control' type={@props.inputType} onChange={@props.updateFn} value={@props.value} defaultValue={@props.defaultValue}>
                    {dropDownOpts}
                </select>

module.exports = DropDown