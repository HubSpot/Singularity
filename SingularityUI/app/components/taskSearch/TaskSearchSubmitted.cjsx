Utils = require '../../utils'
TaskSearchResults = require '../../collections/TaskSearchResults'
LoadingResults = require './LoadingResults'
DisplayResults = require './DisplayResults'

TaskSearchSubmitted = React.createClass

    collectionsReset: (event, response) ->
        @setState({
            loading: false
        })

    getInitialState: ->
        return {
            loading: true
        }

    componentWillMount: ->
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

    render: ->
        if @state.loading
            <LoadingResults />
        else
            <DisplayResults
                collection = @collection
                />


module.exports = TaskSearchSubmitted