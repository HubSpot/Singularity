Utils = require '../../utils'
Enums = require './Enums'
TaskSearchForm = require './TaskSearchForm'
DisplayResults = require './DisplayResults'

TaskSearch = React.createClass

    headerText: 'Task Search'

    countChoices: [5, 10, 25, 50]

    countDefault: 10

    getInitialState: ->
        return {
            requestId: @props.initialRequestId
            deployId: @props.initialDeployId
            host: @props.initialHost
            lastTaskStatus: @props.initialTaskStatus
            startedBefore: @props.initialStartedBefore
            startedAfter: @props.initialStartedAfter
            sortDirection: @props.initialSortDirection or 'ASC'
            pageNumber: 1
            count: @props.initialCount or @countDefault
            showForm: true
        }

    handleSubmit: (event) ->
        event.preventDefault()
        @setState showForm: false

    # Annoying that we need a new function for each property.
    # Unfortuantely using a curried function doesn't seem to work.
    updateReqeustId: (event) ->
        if @props.global
            @setState requestId: event.target.value

    updateDeployId: (event) ->
        @setState deployId: event.target.value

    updateHost: (event) ->
        @setState host: event.target.value

    updateLastTaskStatus: (event) ->
        @setState lastTaskStatus: event.target.value

    updateStartedBefore: (event) ->
        @setState startedBefore: event.date

    updateStartedAfter: (event) ->
        @setState startedAfter: event.date

    updateSortDirection: (event) ->
        @setState sortDirection: event.target.value

    updatePageNumber: (event) ->
        @setState pageNumber: event.target.value

    increasePageNumber: (event) ->
        @setState pageNumber: @state.pageNumber + 1

    setPageNumber: (pageNumber) ->
        if pageNumber > 0
            @setState pageNumber: pageNumber

    decreasePageNumber: (event) ->
        if @state.pageNumber > 1
            @setState pageNumber: @state.pageNumber - 1

    updateCount: (newCount) ->
        @setState count: newCount

    resetForm: ->
        @setState @getInitialState()

    clearRequestId: (event) ->
        if @props.global
            @setState requestId: ''

    clearDeployId: (event) ->
        @setState deployId: ''

    clearHost: (event) ->
        @setState host: ''

    clearSortDirection: (event) ->
        @setState sortDirection: ''

    clearLastTaskStatus: (event) ->
        @setState lastTaskStatus: ''

    clearStartedBefore: (event) ->
        @setState startedBefore: ''

    clearStartedAfter: (event) ->
        @setState startedAfter: ''

    returnToForm: (event) ->
        @setState showForm: true

    render: ->
        if @state.showForm
            return <TaskSearchForm
                header = @header
                handleSubmit = @handleSubmit
                requestId = @state.requestId
                global = @props.global
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
                count = @state.count
                updateCount = @updateCount
                countChoices = @countChoices
                resetForm = @resetForm
            />
        else
            return <DisplayResults
                header = @header
                requestId = @state.requestId
                deployId = @state.deployId
                host = @state.host
                lastTaskStatus = @state.lastTaskStatus
                startedBefore = @state.startedBefore
                startedAfter = @state.startedAfter
                sortDirection = @state.sortDirection
                increasePageNumber = @increasePageNumber
                setPageNumber = @setPageNumber
                decreasePageNumber = @decreasePageNumber
                page = @state.pageNumber
                count = @state.count
                updateCount = @updateCount
                countChoices = @countChoices
                updateSortDirection = @updateSortDirection
                clearRequestId = @clearRequestId
                clearDeployId = @clearDeployId
                clearHost = @clearHost
                clearLastTaskStatus = @clearLastTaskStatus
                clearStartedAfter = @clearStartedAfter
                clearStartedBefore = @clearStartedBefore
                clearSortDirection = @clearSortDirection
                global = @props.global
                returnToForm = @returnToForm
            />


module.exports = TaskSearch