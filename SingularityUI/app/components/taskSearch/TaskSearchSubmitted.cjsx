Utils = require '../../utils'
TaskSearchResults = require '../../collections/TaskSearchResults'
DisplayResults = require './DisplayResults'

TaskSearchSubmitted = React.createClass

    collectionsReset: (event, response) ->
        @setState({
            loading: false
        })

    setPropsCloneEqualToProps: ->
        @propsClone = {
            requestId: @props.requestId
            requestLocked: @props.requestLocked
            deployId: @props.deployId
            host: @props.host
            lastTaskStatus: @props.lastTaskStatus
            startedBefore: @props.startedBefore
            startedAfter: @props.startedAfter
            sortDirection: @props.sortDirection
            page: @props.page
            count: @props.count
            requestLocked: @props.requestLocked
        }

    isPropsCloneEqualToProps: ->
        result = @propsClone.requestId == @props.requestId
        result = result and @propsClone.requestLocked == @props.requestLocked
        result = result and @propsClone.deployId == @props.deployId
        result = result and @propsClone.host == @props.host
        result = result and @propsClone.lastTaskStatus == @props.lastTaskStatus
        result = result and @propsClone.startedBefore == @props.startedBefore
        result = result and @propsClone.startedAfter == @props.startedAfter
        result = result and @propsClone.sortDirection == @props.sortDirection
        result = result and @propsClone.page == @props.page
        result = result and @propsClone.count == @props.count
        result = result and @propsClone.requestLocked == @props.requestLocked
        return result

    getInitialState: ->
        @setPropsCloneEqualToProps()
        return {
            loading: true
        }

    fetchCollection: ->
        @setPropsCloneEqualToProps()
        @collection = new TaskSearchResults [],
            requestId : @props.requestId
            deployId : @props.deployId
            host : @props.host
            lastTaskStatus : @props.lastTaskStatus
            startedAfter : @props.startedAfter
            startedBefore : @props.startedBefore
            orderDirection : @props.sortDirection
            count : @props.count
            page : @props.page
        @collection.on "add", @collectionsReset
        @collection.fetch()

    componentWillMount: ->
        @fetchCollection()

    render: ->
        @fetchCollection() unless @isPropsCloneEqualToProps() or @state.loading
        <DisplayResults
            headerText = @props.headerText
            collection = @collection
            increasePageNumber = @props.increasePageNumber
            decreasePageNumber = @props.decreasePageNumber
            clearRequestId = @props.clearRequestId
            clearDeployId = @props.clearDeployId
            clearHost = @props.clearHost
            clearLastTaskStatus = @props.clearLastTaskStatus
            clearStartedAfter = @props.clearStartedAfter
            clearStartedBefore = @props.clearStartedBefore
            clearSortDirection = @props.clearSortDirection
            requestLocked = @props.requestLocked
        />


module.exports = TaskSearchSubmitted