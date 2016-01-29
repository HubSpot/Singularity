Utils = require '../../utils'
Enums = require './Enums'

FormField = require './FormField'
DropDown = require './DropDown'

TaskSearch = React.createClass

    headerText: 'Search for Tasks'

    getInitialState: ->
        return {
            requestId: @props.initialRequestId
            deployId: @props.initialDeployId
            host: @props.initialHost
            lastTaskStatus: @props.initialTaskStatus
            startedBefore: @props.initialStartedBefore
            startedAfter: @props.initialStartedAfter
            sortDirection: @props.initialSortDirection
        }

    handleSubmit: (event) ->
        console.error(@state)

    # Annoying that we need a new function for each property.
    # Unfortuantely using a curried function doesn't seem to work.
    updateReqeustId: (event) ->
        @setState({
            requestId: event.target.value
        })

    updateDeployId: (event) ->
        @setState({
            deployId: event.target.value
        })

    updateHost: (event) ->
        @setState({
            host: event.target.value
        })

    updateLastTaskStatus: (event) ->
        @setState({
            lastTaskStatus: event.target.value
        })

    updateStartedBefore: (event) ->
        @setState({
            startedBefore: event.target.value
        })

    updateStartedAfter: (event) ->
        @setState({
            startedAfter: event.target.value
        })

    updateSortDirection: (event) ->
        @setState({
            sortDirection: event.target.value
        })

    render: ->
        <div>
            <h2> {@headerText} </h2>
            <form onSubmit={@handleSubmit}>
                <table ><tbody>
                    <FormField 
                        title = 'Request ID' 
                        value = @state.requestId 
                        inputType = 'requestId'
                        disabled = @props.requestLocked
                        updateFn = @updateReqeustId />
                    <FormField 
                        title = 'Deploy ID' 
                        value = @state.deployId 
                        inputType = 'deployId'
                        updateFn = @updateDeployId />
                    <FormField 
                        title = 'Host' 
                        value = @state.host 
                        inputType = 'host'
                        updateFn = @updateHost />
                    <DropDown
                        forceChooseValue = false
                        value = @state.lastTaskStatus
                        choices = Enums.extendedTaskState()
                        inputType = 'lastTaskStatus'
                        title = 'Last Task Status'
                        updateFn = @updateLastTaskStatus />
                    <FormField 
                        title = 'Started Before' 
                        value = @state.startedBefore 
                        inputType = 'startedBefore'
                        updateFn = @updateStartedBefore />
                    <FormField 
                        title = 'Started After' 
                        value = @state.startedAfter 
                        inputType = 'startedAfter'
                        updateFn = @updateStartedAfter />
                    <DropDown
                        forceChooseValue = true
                        value = @state.sortDirection
                        choices = Enums.sortDirections()
                        inputType = 'sortDirection'
                        title = 'Sort Direction'
                        updateFn = @updateSortDirection />
                </tbody></table>
                <button>Search</button>
            </form>
        </div>

module.exports = TaskSearch