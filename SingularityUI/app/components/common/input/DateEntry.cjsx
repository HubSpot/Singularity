FormField = require './FormField'

DateEntry = React.createClass

    componentDidMount: ->
        id = '#' + @props.id
        changeFn = @props.updateFn
        $ -> $(id).datetimepicker({
                sideBySide: true
                format: "ddd MMM DD YYYY HH:mm:ss [UTC]ZZ"
                # This option is of course not documented at all. 
                # Probably because it doesn't work very well.
                # It can be seen in the bootstrap-datetimepicker GitHub.
                # (Thanks Mayuri Sridhar for the suggestion)
                timeZone: moment().format('zz')
            }).on('dp.change', changeFn) # value will be in event.date

    getValue: ->
        return unless @props.value
        time = moment @props.value
        return time.format "ddd MMM DD YYYY HH:mm:ss [UTC]ZZ"

    # MUST pass in UNIQUE id in props.
    # Otherwise the datetime picker will break in ways that aren't even very interesting
    render: ->
        <div className="form-group">
            <div className='input-group date' id={@props.id}>
                <FormField 
                    className = 'form-control'
                    placeholder = {@props.title}
                    type = {@props.inputType}
                    disabled = {@props.disabled}
                    size = {@props.size}
                    value = {@getValue()}
                    updateFn = {@props.updateFn} 
                    id = {@props.id}
                />
                <span className="input-group-addon">
                    <span className="glyphicon glyphicon-calendar"></span>
                </span>
            </div>
        </div>



module.exports = DateEntry