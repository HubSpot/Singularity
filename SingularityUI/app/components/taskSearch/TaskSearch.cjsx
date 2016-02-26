React = require 'react'
Utils = require '../../utils'
Enums = require './Enums'
TaskSearchForm = require './TaskSearchForm'
DisplayResults = require './DisplayResults'
Header = require './Header'

TaskSearch = React.createClass

    countChoices: [5, 10, 25, 100]

    defaultCount: 10

    defaultSortDirection: 'DESC'

    getInitialState: ->
        return {
            form:
                requestId: @props.initialRequestId or ''
                deployId: @props.initialDeployId or ''
                host: @props.initialHost or ''
                lastTaskStatus: @props.initialTaskStatus or ''
                startedBefore: @props.initialStartedBefore or ''
                startedAfter: @props.initialStartedAfter or ''
            sortDirection: @props.initialSortDirection or @defaultSortDirection
            queryParams:
                requestId: @props.initialRequestId or ''
            pageNumber: 1
            count: @props.initialCount or @defaultCount
        }

    handleSubmit: (event) ->
        event.preventDefault()
        @setState
            queryParams: @state.form
            pageNumber: 1 # If you narrow down your search you most likely want to go back to page 1

    # Annoying that we need a new function for each property.
    # Unfortuantely using a curried function doesn't seem to work.
    updateReqeustId: (event) ->
        if @props.global
            form = $.extend {}, @state.form
            form.requestId = event.target.value
            @setState
                form: form

    updateDeployId: (event) ->
        form = $.extend {}, @state.form
        form.deployId = event.target.value
        @setState
            form: form

    updateHost: (event) ->
        form = $.extend {}, @state.form
        form.host = event.target.value
        @setState
            form: form

    updateLastTaskStatus: (event) ->
        form = $.extend {}, @state.form
        form.lastTaskStatus = event.target.value
        @setState
            form: form

    updateStartedBefore: (event) ->
        form = $.extend {}, @state.form
        form.startedBefore = event.date
        @setState
            form: form

    updateStartedAfter: (event) ->
        form = $.extend {}, @state.form
        form.startedAfter = event.date
        @setState
            form: form

    resetForm: ->
        @setState @getInitialState()

    updateSortDirection: (event) ->
        if @state.sortDirection is Enums.sortDirections()[0].value
            @setState sortDirection: Enums.sortDirections()[1].value
        else
            @setState sortDirection: Enums.sortDirections()[0].value

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

    render: ->
        <div>
            <Header
                global = @props.global
                requestId = @props.initialRequestId
            />
            <h2> Search Parameters </h2>
            <TaskSearchForm
                handleSubmit = @handleSubmit
                requestId = @state.form.requestId
                requestIdCurrentSearch = @state.queryParams.requestId
                global = @props.global
                updateReqeustId = @updateReqeustId
                deployId = @state.form.deployId
                updateDeployId = @updateDeployId
                deployIdCurrentSearch = @state.queryParams.deployId
                host = @state.form.host
                updateHost = @updateHost
                hostCurrentSearch = @state.queryParams.host
                lastTaskStatus = @state.form.lastTaskStatus
                updateLastTaskStatus = @updateLastTaskStatus
                lastTaskStatusCurrentSearch = @state.queryParams.lastTaskStatus
                startedAfter = @state.form.startedAfter
                updateStartedAfter = @updateStartedAfter
                startedAfterCurrentSearch = {@state.queryParams.startedAfter.format window.config.timestampFormat if @state.queryParams.startedAfter}
                startedBefore = @state.form.startedBefore
                updateStartedBefore = @updateStartedBefore
                startedBeforeCurrentSearch = {@state.queryParams.startedBefore.format window.config.timestampFormat if @state.queryParams.startedBefore}
                resetForm = @resetForm
            />
            <h2>Tasks</h2>
            <DisplayResults
                requestId = @state.queryParams.requestId
                deployId = @state.queryParams.deployId
                host = @state.queryParams.host
                lastTaskStatus = @state.queryParams.lastTaskStatus
                startedAfter = @state.queryParams.startedAfter
                startedBefore = @state.queryParams.startedBefore
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
            />
        </div>


module.exports = TaskSearch
