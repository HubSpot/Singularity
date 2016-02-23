Utils = require '../../utils'
Enums = require './Enums'
TaskSearchForm = require './TaskSearchForm'
DisplayResults = require './DisplayResults'
Header = require './Header'

TaskSearch = React.createClass

    headerText: 'Task Search'

    countChoices: [5, 10, 25, 50]

    countDefault: 10

    getInitialState: ->
        return {
            form:
                requestId: @props.initialRequestId or ''
                deployId: @props.initialDeployId or ''
                host: @props.initialHost or ''
                lastTaskStatus: @props.initialTaskStatus or ''
                startedBefore: @props.initialStartedBefore or ''
                startedAfter: @props.initialStartedAfter or ''
            sortDirection: @props.initialSortDirection or 'ASC'
            queryParams:
                requestId: @props.initialRequestId or ''
            pageNumber: 1
            count: @props.initialCount or @countDefault
            showForm: true
        }

    handleSubmit: (event) ->
        event.preventDefault()
        @setState 
            showForm: false
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
        @setState 
            form: @getInitialState().form

    resetFormToCurrentParams: (event) ->
        @setState
            form: @state.queryParams

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
        <div>
            <Header
                global = @props.global
                requestId = @props.initialRequestId
            />
            <TaskSearchForm
                header = @header
                handleSubmit = @handleSubmit
                requestId = @state.form.requestId
                global = @props.global
                updateReqeustId = @updateReqeustId
                deployId = @state.form.deployId
                updateDeployId = @updateDeployId
                host = @state.form.host
                updateHost = @updateHost
                lastTaskStatus = @state.form.lastTaskStatus
                updateLastTaskStatus = @updateLastTaskStatus
                startedBefore = @state.form.startedBefore
                updateStartedBefore = @updateStartedBefore
                startedAfter = @state.form.startedAfter
                updateStartedAfter = @updateStartedAfter
                resetForm = @resetForm
                resetFormToCurrentParams = @resetFormToCurrentParams
            />
            <DisplayResults
                header = @header
                requestId = @state.queryParams.requestId
                deployId = @state.queryParams.deployId
                host = @state.queryParams.host
                lastTaskStatus = @state.queryParams.lastTaskStatus
                startedBefore = @state.queryParams.startedBefore
                startedAfter = @state.queryParams.startedAfter
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
        </div>


module.exports = TaskSearch
