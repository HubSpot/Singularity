Utils = require '../../utils'

FormField = require '../common/input/FormField'
DropDown = require '../common/input/DropDown'
DateEntry = require '../common/input/DateEntry'
Enums = require './Enums'
Header = require './Header'

TaskSearchForm = React.createClass

    updateCount: (event) ->
        @props.updateCount(event.target.value)

    render: ->
        <div className='col-xs-5'>
            <Header
                global = @props.global
                requestId = @props.requestId
            />
            <form role="form" onSubmit={@props.handleSubmit}>
                <div className='form-group'>
                    <label htmlFor="requestId">Request ID:</label>
                    <FormField 
                        value = @props.requestId 
                        inputType = 'text'
                        id = 'requestId'
                        disabled = {'disabled' unless @props.global}
                        updateFn = @props.updateReqeustId />
                </div>
                <div className='form-group'>
                    <label htmlFor="deployId">Deploy ID:</label>
                    <FormField
                        value = @props.deployId 
                        inputType = 'text'
                        id = 'deployId'
                        updateFn = @props.updateDeployId />
                </div>
                <div className='form-group'>
                    <label htmlFor="host">Host:</label>
                    <FormField
                        value = @props.host 
                        inputType = 'text'
                        id = 'host'
                        updateFn = @props.updateHost />
                </div>
                <div className='form-group'>
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
                <div className='form-group'>
                    <label htmlFor="startedBefore">Started Before:</label>
                    <DateEntry 
                        value = @props.startedBefore
                        inputType = 'date'
                        id = 'startedBefore'
                        updateFn = @props.updateStartedBefore />
                </div>
                <div className='form-group'>
                    <label htmlFor="startedAfter">Started After:</label>
                    <DateEntry 
                        value = @props.startedAfter 
                        inputType = 'date'
                        id = 'startedAfter'
                        updateFn = @props.updateStartedAfter />
                </div>
                <div className='form-group'>
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
                <div className='form-group'>
                    <label htmlFor="count">Tasks Per Page:</label>
                    <DropDown
                        forceChooseValue = true
                        value = @props.count
                        choices = {@props.countChoices}
                        inputType = 'number'
                        id = 'count'
                        title = 'Tasks Per Page'
                        updateFn = @updateCount />
                </div>
                <button type="button" className="btn btn-danger" onClick={@props.resetForm}>Clear Form</button>
                <button type="submit" className="btn btn-primary pull-right">Search</button>
            </form>
        </div>

module.exports = TaskSearchForm
