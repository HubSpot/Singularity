FormField = require './FormField'

DateEntry = React.createClass

    blah: ->
        'startedAfter'

    componentDidMount: ->
        id = '#' + @props.id
        $ -> $(id).datetimepicker()

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
                                onChange = {@props.updateFn} 
                                value = {@props.value}
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