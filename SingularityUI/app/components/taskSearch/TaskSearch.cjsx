Utils = require '../../utils'
Enums = require './Enums'
TaskSearchForm = require './TaskSearchForm'
TaskSearchSubmitted = require './TaskSearchSubmitted'

TaskSearch = React.createClass

    headerText: 'Task Search'

    getInitialState: ->
        return {
            requestId: @props.initialRequestId
            deployId: @props.initialDeployId
            host: @props.initialHost
            lastTaskStatus: @props.initialTaskStatus
            startedBefore: @props.initialStartedBefore
            startedAfter: @props.initialStartedAfter
            sortDirection: @props.initialSortDirection
            pageNumber: 1
            showForm: true
        }

    handleSubmit: (event) ->
        event.preventDefault()
        @setState({
            showForm: false
        })

    # Annoying that we need a new function for each property.
    # Unfortuantely using a curried function doesn't seem to work.
    updateReqeustId: (event) ->
        if not @props.requestLocked
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
            startedBefore: event.date
        })

    updateStartedAfter: (event) ->
        @setState({
            startedAfter: event.date
        })

    updateSortDirection: (event) ->
        @setState({
            sortDirection: event.target.value
        })

    updatePageNumber: (event) ->
        @setState({
            pageNumber: event.target.value
        })

    increasePageNumber: (event) ->
        @setState({
            pageNumber: @state.pageNumber + 1
        })

    decreasePageNumber: (event) ->
        if @state.pageNumber > 1
            @setState({
                pageNumber: @state.pageNumber - 1
            })

    resetForm: ->
        @setState(@getInitialState())

    clearRequestId: (event) ->
        if not @props.requestLocked
            @setState({
                requestID: ''
            })

    clearDeployId: (event) ->
        @setState({
            deployId: ''
        })

    clearHost: (event) ->
        @setState({
            host: ''
        })

    clearSortDirection: (event) ->
        @setState({
            sortDirection: ''
        })

    clearLastTaskStatus: (event) ->
        @setState({
            lastTaskStatus: ''
        })

    clearStartedBefore: (event) ->
        @setState({
            startedBefore: ''
        })

    clearStartedAfter: (event) ->
        @setState({
            startedAfter: ''
        })

    render: ->
        if @state.showForm
            return <TaskSearchForm
                headerText = @headerText
                handleSubmit = @handleSubmit
                requestId = @state.requestId
                requestLocked = @props.requestLocked
                updateReqeustId = @updateReqeustId
                deployId = @state.deployId
                updateDeployId = @updateDeployId
                host = @state.host
                updateHost = @updateHost
                lastTaskStatus = @state.lastTaskStatus
                updateLastTaskStatus = @updateLastTaskStatus
                startedBefore = @state.startedBefore
                updateStartedBefore = @updateStartedBefore
                startedAfter = @state.startedAfter
                updateStartedAfter = @updateStartedAfter
                sortDirection = @state.sortDirection
                updateSortDirection = @updateSortDirection
                resetForm = @resetForm
            />
        else
            return <TaskSearchSubmitted
                headerText = @headerText
                requestId = @state.requestId
                requestLocked = @state.requestLocked
                deployId = @state.deployId
                host = @state.host
                lastTaskStatus = @state.lastTaskStatus
                startedBefore = @state.startedBefore
                startedAfter = @state.startedAfter
                sortDirection = @state.sortDirection
                increasePageNumber = @increasePageNumber
                decreasePageNumber = @decreasePageNumber
                page = @state.pageNumber
                count = 20
                clearRequestId = @clearRequestId
                clearDeployId = @clearDeployId
                clearHost = @clearHost
                clearLastTaskStatus = @clearLastTaskStatus
                clearStartedAfter = @clearStartedAfter
                clearStartedBefore = @clearStartedBefore
                clearSortDirection = @clearSortDirection
                requestLocked = @props.requestLocked
            />


module.exports = TaskSearch