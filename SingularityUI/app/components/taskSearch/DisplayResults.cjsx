TaskSearchResults = require '../../collections/TaskSearchResults'

QueryParam = require './QueryParam'
Task = require './Task'

StripedTable = require '../common/Table'
TimeStamp = require '../common/TimeStamp'
TaskStateLabel = require '../common/TaskStateLabel'
Link = require '../common/Link'

DisplayResults = React.createClass

    collectionsReset: (event, response) ->
        @setState({
            loading: false
        })

    # Used to detect if any props have changed
    didPropsChange: (nextProps) ->
        result = nextProps.requestId != @props.requestId
        result = result or nextProps.requestLocked != @props.requestLocked
        result = result or nextProps.deployId != @props.deployId
        result = result or nextProps.host != @props.host
        result = result or nextProps.lastTaskStatus != @props.lastTaskStatus
        result = result or nextProps.startedBefore != @props.startedBefore
        result = result or nextProps.startedAfter != @props.startedAfter
        result = result or nextProps.sortDirection != @props.sortDirection
        result = result or nextProps.page != @props.page
        result = result or nextProps.count != @props.count
        result = result or nextProps.requestLocked != @props.requestLocked
        return result

    getInitialState: ->
        @willFetch = false
        return {
            loading: true
        }

    fetchCollection: ->
        @willFetch = false
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

    componentWillReceiveProps: (nextProps) ->
        if @didPropsChange nextProps
            @willFetch = true

    getQueryParams: ->
        params = []
        key = 0
        if @collection.requestId
            params.push(<div key={key}> <QueryParam
                paramName = "Request Id"
                paramValue = @collection.requestId
                onClick = @props.clearRequestId
                cantClear = @props.requestLocked
                /></div>)
            key++
        if @collection.deployId
            params.push(<div key={key}> <QueryParam
                paramName = "Deploy Id"
                paramValue = @collection.deployId
                onClick = @props.clearDeployId
                /></div>)
            key++
        if @collection.host
            params.push(<div key={key}> <QueryParam
                paramName = "Host"
                paramValue = @collection.host
                onClick = @props.clearHost
                /></div>)
            key++
        if @collection.lastTaskStatus
            params.push(<div key={key}> <QueryParam
                paramName = "Last Task Status"
                paramValue = @collection.lastTaskStatus
                onClick = @props.clearLastTaskStatus
                /></div>)
            key++
        if @collection.startedAfter
            params.push(<div key={key}> <QueryParam
                paramName = "Started After"
                paramValue = @collection.startedAfter._d.toString()
                onClick = @props.clearStartedAfter
                /></div>)
            key++
        if @collection.startedBefore
            params.push(<div key={key}> <QueryParam
                paramName = "Started Before"
                paramValue = @collection.startedBefore._d.toString()
                onClick = @props.clearStartedBefore
                /></div>)
            key++
        if @collection.sortDirection
            params.push(<div key={key}> <QueryParam
                paramName = "Sort Direction"
                paramValue = @collection.sortDirection
                onClick = @props.clearSortDirection
                /></div>)
            key++
        return params

    # using className="previous" for the next button is necessary to align
    # it to the left side of the page. This is built into bootstrap
    renderPageButtons: ->
        <nav>
            <ul className="pager">
                <li className={@collection.page == 1 and "previous disabled" or "previous"} onClick={@props.decreasePageNumber}><a href="#">Previous</a></li>
                <li className="previous disabled"><a href="#">Page {@collection.page}</a></li>
                <li className="previous" onClick={@props.increasePageNumber}><a href="#">Next</a></li>
            </ul>
        </nav>


    renderTasks: ->
        taskTableColumns = ["Name", "Last State", "Deploy", "Started", "Updated"]
        taskTableData = []
        for task in @collection.models
            taskTableData.push([<Link
                                    text={task.taskId.id}
                                    url={window.config.appRoot + "/task/" + task.taskId.id}
                                    altText={task.taskId.id}
                                />, 
                                <TaskStateLabel
                                    taskState={task.lastTaskState}
                                />,
                                <Link
                                    text={task.taskId.deployId}
                                    url={window.config.appRoot + "/request/" + task.taskId.requestId + "/deploy/" + task.taskId.deployId}
                                />, 
                                <TimeStamp 
                                    timestamp={task.taskId.startedAt} 
                                    display='timeStampFromNow'} 
                                />, 
                                <TimeStamp 
                                    timestamp={task.updatedAt} 
                                    display='timeStampFromNow'} 
                                />])
        return <StripedTable
                    tableClassOpts="table-striped"
                    columnNames={taskTableColumns}
                    tableRows={taskTableData}
                    />


    render: ->
        @fetchCollection() if @willFetch
        <div>
            <h1>{@props.headerText}</h1>
            <h2>Query Parameters</h2>
            {@getQueryParams()}
            <br />
            <button className="btn btn-danger" onClick=@props.returnToForm>Modify Query Parameters</button>
            <h2>Tasks</h2>
            {@renderPageButtons()}
            {@renderTasks()}
            {@renderPageButtons()}
        </div>

module.exports = DisplayResults