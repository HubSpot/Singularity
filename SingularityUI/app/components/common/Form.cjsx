FormField = require './formItems/FormField'
React = require 'react'
classNames = require 'classnames'

Form = React.createClass

    renderFormGroups: ->
        groups = []
        @props.formGroups.map (group, field) =>
            ComponentClass = group.component
            groups.push (
                <div className="col-md-#{@props.inputSize}" key={field}>
                    <div className='form-group'>
                        <label htmlFor=group.id>{group.title}</label>
                        <ComponentClass
                            id = group.id
                            prop = group.prop />
                    </div>
                </div>)
        return groups

    render: ->
        <form role='form' onSubmit={@props.handleSubmit} className={@props.className}>
            {@renderFormGroups()}
            <label htmlFor='buttons'> </label>
            <div id='buttons' className={classNames @props.buttonsClass, "col-md-#{@props.inputSize}", 'row'}>
                {<div className='col-md-3'>
                    <button type='button' className='btn btn-danger' onClick=@props.resetForm>Clear</button>
                </div> if @props.resetForm}
                {<div className='col-md-3'>
                    <button type='button' className='btn btn-danger' onClick=@props.cancel>Cancel</button>
                </div> if @props.cancel}
                <div className='pull-right'>
                    <div className='col-md-3'>
                        <button type='submit' className='btn btn-primary'>{@props.submitButtonText}</button>
                    </div>
                </div>
            </div>
        </form>

module.exports = Form
