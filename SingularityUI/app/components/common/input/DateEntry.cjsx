FormField = require './FormField'

DateEntry = React.createClass

    blah: ->
        'startedAfter'

    componentDidMount: ->
        id = '#' + @props.id
        changeFn = @props.updateFn
        $ -> $(id).datetimepicker({
                sideBySide: true
                format: "ddd MMM DD YYYY HH:mm:ss [UTC]ZZ"
            }).on('dp.change', changeFn) # value will be in event.date

    render: ->
        <div className="container">
            <div className="row">
                <div className='col-sm-6'>
                    <div className="form-group">
                        <div className='input-group date' id={@props.id}>
                            <FormField 
                                className = 'form-control'
                                placeholder = {@props.title}
                                type = {@props.inputType}
                                disabled = {@props.disabled}
                                size = {@props.size} 
                                id = {@props.id}
                            />
                            <span className="input-group-addon">
                                <span className="glyphicon glyphicon-calendar"></span>
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>



module.exports = DateEntry