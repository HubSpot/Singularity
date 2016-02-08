Utils = require '../../utils'

FormField = require '../common/input/FormField'
DropDown = require '../common/input/DropDown'
DateEntry = require '../common/input/DateEntry'
Enums = require './Enums'

TaskSearchForm = React.createClass

    render: ->
        <div className='col-xs-5'>
            <h2> {@props.headerText} </h2>
            <form role="form" onSubmit={@props.handleSubmit}>
                <div className='formGroup'>
                    <label htmlFor="requestId">Request ID:</label>
                    <FormField 
                        value = @props.requestId 
                        inputType = 'text'
                        id = 'requestId'
                        disabled = {'disabled' if @props.requestLocked}
                        updateFn = @props.updateReqeustId />
                </div>
                <br />
                <div className='formGroup'>
                    <label htmlFor="deployId">Deploy ID:</label>
                    <FormField
                        value = @props.deployId 
                        inputType = 'text'
                        id = 'deployId'
                        updateFn = @props.updateDeployId />
                </div>
                <br />
                <div className='formGroup'>
                    <label htmlFor="host">Host:</label>
                    <FormField
                        value = @props.host 
                        inputType = 'text'
                        id = 'host'
                        updateFn = @props.updateHost />
                </div>
                <br />
                <div className='formGroup'>
                    <label htmlFor="lastTaskStatus">Last Task Status:</label>
                    <DropDown
                        forceChooseValue = false
                        value = @props.lastTaskStatus
                        choices = Enums.extendedTaskState()
                        inputType = 'lastTaskStatus'
                        title = 'Last Task Status'
                        id = 'lastTaskStatus'
                        updateFn = @props.updateLastTaskStatus />
                </div>
                <br />
                <div className='formGroup'>
                    <label htmlFor="startedBefore">Started Before:</label>
                    <DateEntry 
                        value = @props.startedBefore
                        inputType = 'date'
                        id = 'startedBefore'
                        updateFn = @props.updateStartedBefore />
                </div>
                <div className='formGroup'>
                    <label htmlFor="startedAfter">Started After:</label>
                    <DateEntry 
                        value = @props.startedAfter 
                        inputType = 'date'
                        id = 'startedAfter'
                        updateFn = @props.updateStartedAfter />
                </div>
                <div className='formGroup'>
                    <label htmlFor="sortDirection">Sort Direction:</label>
                    <DropDown
                        forceChooseValue = true
                        value = @props.sortDirection
                        choices = Enums.sortDirections()
                        inputType = 'sortDirection'
                        id = 'sortDirection'
                        title = 'Sort Direction'
                        updateFn = @props.updateSortDirection />
                </div>
                <br />
                <button type="button" className="btn btn-danger" onClick={@props.resetForm}>Clear Form</button>
                <button type="submit" className="btn btn-primary navbar-right">Search</button>
            </form>
        </div>

module.exports = TaskSearchForm