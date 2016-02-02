Utils = require '../../utils'

FormField = require './FormField'
DropDown = require './DropDown'
Enums = require './Enums'

TaskSearchForm = React.createClass

	render: ->
		<div>
            <h2> {@props.headerText} </h2>
            <form onSubmit={@props.handleSubmit}>
                <table ><tbody>
                    <FormField 
                        title = 'Request ID' 
                        value = @props.requestId 
                        inputType = 'requestId'
                        disabled = @props.requestLocked
                        updateFn = @props.updateReqeustId />
                    <FormField 
                        title = 'Deploy ID' 
                        value = @props.deployId 
                        inputType = 'deployId'
                        updateFn = @props.updateDeployId />
                    <FormField 
                        title = 'Host' 
                        value = @props.host 
                        inputType = 'host'
                        updateFn = @props.updateHost />
                    <DropDown
                        forceChooseValue = false
                        value = @props.lastTaskStatus
                        choices = Enums.extendedTaskState()
                        inputType = 'lastTaskStatus'
                        title = 'Last Task Status'
                        updateFn = @props.updateLastTaskStatus />
                    <FormField 
                        title = 'Started Before' 
                        value = @props.startedBefore 
                        inputType = 'startedBefore'
                        updateFn = @props.updateStartedBefore />
                    <FormField 
                        title = 'Started After' 
                        value = @props.startedAfter 
                        inputType = 'startedAfter'
                        updateFn = @props.updateStartedAfter />
                    <DropDown
                        forceChooseValue = true
                        value = @props.sortDirection
                        choices = Enums.sortDirections()
                        inputType = 'sortDirection'
                        title = 'Sort Direction'
                        updateFn = @props.updateSortDirection />
                </tbody></table>
                <button>Search</button>
            </form>
        </div>

module.exports = TaskSearchForm