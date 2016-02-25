React = require 'react'
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

    dropDownOpts: ->
        @props.prop.choices.map (element, key) =>
            <option key={key} value={@getValue(element)}>{@getUserReadable(element)}</option>

    # Pass in choices to @props.prop.choices as 
    # an array of primitives, objects with user and value 
    # (where user is the user readable text and value is 
    #  the value that is returned if that choice is chosen)
    # or a mixture
    render: ->
        return <select  id=@props.id 
                        className = {classNames 'form-control', @props.prop.customClass}
                        type = {@props.prop.inputType} 
                        onChange = {@props.prop.updateFn} 
                        value = {@props.prop.value} 
                        defaultValue = {@props.prop.defaultValue}
                        required = {if @props.prop.required then true else false}>
                    {<option key=0 value='' /> unless @props.prop.forceChooseValue}
                    {@dropDownOpts()}
                </select>

module.exports = DropDown
