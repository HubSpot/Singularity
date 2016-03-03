React = require 'react'
TaskSearchResults = require '../../collections/TaskSearchResults'

QueryParameters = require '../common/QueryParameters'
FormField = require '../common/formItems/FormField'
DropDown = require '../common/formItems/DropDown'
TaskTable = require '../common/TaskTable'

Enums = require './Enums'

DisplayResults = React.createClass

    # Used to detect if any props have changed
    didPropsChange: (nextProps) ->
        return true unless nextProps.requestId == @props.requestId
        return true unless nextProps.global == @props.global
        return true unless nextProps.deployId == @props.deployId
        return true unless nextProps.host == @props.host
        return true unless nextProps.lastTaskStatus == @props.lastTaskStatus
        return true unless nextProps.startedBefore == @props.startedBefore
        return true unless nextProps.startedAfter == @props.startedAfter
        return true unless nextProps.sortDirection == @props.sortDirection
        return true unless nextProps.page == @props.page
        return true unless nextProps.count == @props.count
        return false

    getInitialState: ->
        @willFetch = false
        return {
            loading: true
        }

    getEmptyTableMessage: ->
        if @props.holdOffOnSearching
            '''
                No Search Performed Yet. 
                You may perform a search with no query parameters.
                Please be aware that a search without Request ID is VERY expensive.
            '''
        else if @state.loading
            'Loading Tasks...'
        else
            'No Tasks Found'

    fetchCollection: ->
        unless @props.holdOffOnSearching
            @willFetch = false
        @collection = new TaskSearchResults [],
            params: {
                requestId : @props.requestId
                deployId : @props.deployId
                host : @props.host
                lastTaskStatus : @props.lastTaskStatus
                startedBefore : @props.startedBefore.valueOf() if @props.startedBefore
                startedAfter : @props.startedAfter.valueOf() if @props.startedAfter
                orderDirection : @props.sortDirection
                count : @props.count
                page : @props.page
            }
        unless @props.holdOffOnSearching
            @collection.fetch
                success: () => @setState {loading: false}

    componentWillMount: ->
        @fetchCollection()

    componentWillReceiveProps: (nextProps) ->
        if @didPropsChange nextProps
            @willFetch = true
            @setState
                loading: true

    renderPageNavBar: ->
        <TableNavigationBar
            currentPage = @collection.params.page
            decreasePageNumber = @props.decreasePageNumber
            increasePageNumber = @props.increasePageNumber
            setPageNumber = @props.setPageNumber
            numberPerPage = @props.count
            objectsBeingDisplayed = "Tasks"
            numberPerPageChoices = @props.countChoices
            setNumberPerPage = @props.updateCount
            sortDirection = @props.sortDirection
            sortDirectionChoices = Enums.sortDirections()
            setSortDirection = @props.updateSortDirection
        />

    render: ->
        @fetchCollection() if @willFetch
        <div className='col-xl-12'>
            <TaskTable
                models = {@collection.models}
                sortDirection = @props.sortDirection
                sortDirectionAscending = Enums.sortDirections()[0].value
                sortBy = 'Started'
                sortableByStarted = true
                sortByStarted = @props.updateSortDirection
                rowsPerPageChoices = @props.countChoices
                setRowsPerPage = @props.updateCount
                pageNumber = @collection.params.page
                pageDown = @props.decreasePageNumber
                pageUp = @props.increasePageNumber
                emptyTableMessage = {@getEmptyTableMessage()}
            />
        </div>

module.exports = DisplayResults
