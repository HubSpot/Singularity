QueryParam = require './QueryParam'
Task = require './Task'
StripedTable = require '../common/Table'
TimeStamp = require '../common/TimeStamp'
TaskStateLabel = require '../common/TaskStateLabel'
Link = require '../common/Link'

DisplayResults = React.createClass

    getQueryParams: ->
        params = []
        key = 0
        if @props.collection.requestId
            params.push(<div key={key}> <QueryParam
                paramName = "Request Id"
                paramValue = @props.collection.requestId
                onClick = @props.clearRequestId
                cantClear = @props.requestLocked
                /></div>)
            key++
        if @props.collection.deployId
            params.push(<div key={key}> <QueryParam
                paramName = "Deploy Id"
                paramValue = @props.collection.deployId
                onClick = @props.clearDeployId
                /></div>)
            key++
        if @props.collection.host
            params.push(<div key={key}> <QueryParam
                paramName = "Host"
                paramValue = @props.collection.host
                onClick = @props.clearHost
                /></div>)
            key++
        if @props.collection.lastTaskStatus
            params.push(<div key={key}> <QueryParam
                paramName = "Last Task Status"
                paramValue = @props.collection.lastTaskStatus
                onClick = @props.clearLastTaskStatus
                /></div>)
            key++
        if @props.collection.startedAfter
            params.push(<div key={key}> <QueryParam
                paramName = "Started After"
                paramValue = @props.collection.startedAfter._d.toString()
                onClick = @props.clearStartedAfter
                /></div>)
            key++
        if @props.collection.startedBefore
            params.push(<div key={key}> <QueryParam
                paramName = "Started Before"
                paramValue = @props.collection.startedBefore._d.toString()
                onClick = @props.clearStartedBefore
                /></div>)
            key++
        if @props.collection.sortDirection
            params.push(<div key={key}> <QueryParam
                paramName = "Sort Direction"
                paramValue = @props.collection.sortDirection
                onClick = @props.clearSortDirection
                /></div>)
            key++
        return params

    # using className="previous" for the next button is necessary to align
    # it to the left side of the page. This is built into bootstrap
    renderPageButtons: ->
        <nav>
            <ul className="pager">
                <li className={@props.collection.page == 1 and "previous disabled" or "previous"} onClick={@props.decreasePageNumber}><a href="#">Previous</a></li>
                <li className="previous disabled"><a href="#">Page {@props.collection.page}</a></li>
                <li className="previous" onClick={@props.increasePageNumber}><a href="#">Next</a></li>
            </ul>
        </nav>


    renderTasks: ->
        taskTableColumns = ["Name", "Last State", "Deploy", "Started", "Updated"]
        taskTableData = []
        for task in @props.collection.models
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
        return <div>
            <h1>{@props.headerText}</h1>
            <h2>Query Params</h2>
            {@getQueryParams()}
            <h2>Tasks</h2>
            {@renderPageButtons()}
            {@renderTasks()}
            {@renderPageButtons()}
        </div>

module.exports = DisplayResults