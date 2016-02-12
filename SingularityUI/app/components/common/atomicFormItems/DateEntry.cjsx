FormField = require './FormField'
Glyphicon = require '../atomicDisplayItems/Glyphicon'

DateEntry = React.createClass

    componentDidMount: ->
        id = '#' + @props.id
        changeFn = @props.prop.updateFn
        $ -> $(id).datetimepicker({
                sideBySide: true
                format: window.config.timestampWithSecondsFormat
                # This option is of course not documented at all. 
                # Probably because it doesn't work very well.
                # It can be seen in the bootstrap-datetimepicker GitHub.
                # (Thanks Mayuri Sridhar for the suggestion)
                timeZone: moment().format('zz')
            }).on('dp.change', changeFn) # value will be in event.date

    getValue: ->
        return unless @props.prop.value
        time = moment @props.prop.value
        return time.format window.config.timestampWithSecondsFormat

    # MUST pass in UNIQUE id in props.
    # Otherwise the datetime picker will break in ways that aren't even very interesting
    render: ->
        <div className="form-group">
            <div className='input-group date' id={@props.id}>
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
                <span className="input-group-addon">
                    <Glyphicon iconClass="glyphicon-calendar"/>
                </span>
            </div>
        </div>



module.exports = DateEntry
