React = require 'react'
moment = require 'moment'
FormField = require './FormField'
Glyphicon = require '../atomicDisplayItems/Glyphicon'
datetimepicker = require 'eonasdan-bootstrap-datetimepicker'

DateEntry = React.createClass

    componentWillReceiveProps: (nextProps) ->
        id = '#' + @props.id
        datetimepicker = $(id).data('datetimepicker')
        if datetimepicker
            if nextProps.prop.value isnt props.prop.value
                datetimepicker.setDate(null) unless nextProps.prop.value



    initializeDateTimePicker: ->
        id = '#' + @props.id
        changeFn = @props.prop.updateFn
        $ -> $(id).datetimepicker({
                sideBySide: true
                format: window.config.timestampFormat
            }).on('dp.change', changeFn) # value will be in event.date

    getValue: ->
        return unless @props.prop.value
        time = moment @props.prop.value
        return time.format window.config.timestampFormat

    # MUST pass in UNIQUE id in props.
    # Otherwise the datetime picker will break in ways that aren't even very interesting
    render: ->
        <div className="input-group date #{@props.prop.customClass}" id={@props.id}>
            <FormField 
                id = {@props.id}
                prop = {{
                    updateFn: @props.prop.updateFn
                    value: @getValue()
                    size: @props.prop.size
                    disabled: @props.prop.disabled
                    type: @props.prop.inputType
                    placeholder: @props.prop.placeholder
                    required: @props.prop.required
                    customClass: @props.prop.customClass
                }}
            />
            <span className='input-group-addon' onMouseOver={@initializeDateTimePicker}>
                <Glyphicon iconClass='calendar'/>
            </span>
        </div>



module.exports = DateEntry
