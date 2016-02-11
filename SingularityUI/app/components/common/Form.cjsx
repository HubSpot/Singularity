FormField = require './atomicFormItems/FormField'

Form = React.createClass

    renderFormGroups: ->
        @props.formGroups.map (group, key) =>
            ComponentClass = group.component
            #debugger
            return <div className='form-group' key={key}>
                <label htmlFor=group.id>{group.title}:</label>
                <ComponentClass
                    id = group.id
                    prop = group.prop />
            </div>

    render: ->
        <form role="form" onSubmit={@props.handleSubmit} className={@props.className}>
            {@renderFormGroups()}
            {<button type="button" className="btn btn-danger" onClick=@props.resetForm>Clear Form</button> if @props.resetForm}
            {<button type="button" className="btn btn-danger" onClick=@props.cancel>Cancel</button> if @props.cancel}
            <button type="submit" className="btn btn-primary pull-right">{@props.submitButtonText}</button>
        </form>

module.exports = Form
