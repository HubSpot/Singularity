Utils = require '../../../utils'

DropDown = React.createClass

    getValue: (element) ->
        if typeof element is 'object'
            return element.value
        else
            return element

    getUserReadable: (element) ->
        if typeof element is 'object'
            return element.user
        else
            return element

    # Pass in choices to @props.choices as 
    # an array of primitives, objects with user and value 
    # (where user is the user readable text and value is 
    #  the value that is returned if that choice is chosen)
    # or a mixture
    render: ->
        dropDownOpts = []
        dropDownOpts.push(<option key=0 value=''></option>) unless @props.forceChooseValue
        i = 1
        for element in @props.choices
            dropDownOpts.push(<option key={i} value={@getValue(element)}>{@getUserReadable(element)}</option>)
            i++
        return <select className='form-control' type={@props.inputType} onChange={@props.updateFn} value={@props.value} defaultValue={@props.defaultValue}>
                    {dropDownOpts}
                </select>

module.exports = DropDown
