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
                /></div>)
            key++
        if @props.collection.deployId
            params.push(<div key={key}> <QueryParam
                paramName = "Deploy Id"
                paramValue = @props.collection.deployId
                /></div>)
            key++
        if @props.collection.host
            params.push(<div key={key}> <QueryParam
                paramName = "Host"
                paramValue = @props.collection.host
                /></div>)
            key++
        if @props.collection.lastTaskStatus
            params.push(<div key={key}> <QueryParam
                paramName = "Last Task Status"
                paramValue = @props.collection.lastTaskStatus
                /></div>)
            key++
        if @props.collection.startedAfter
            params.push(<div key={key}> <QueryParam
                paramName = "Started After"
                paramValue = @props.collection.startedAfter._d.toString()
                /></div>)
            key++
        if @props.collection.startedBefore
            params.push(<div key={key}> <QueryParam
                paramName = "Started Before"
                paramValue = @props.collection.startedBefore._d.toString()
                /></div>)
            key++
        if @props.collection.sortDirection
            params.push(<div key={key}> <QueryParam
                paramName = "Sort Direction"
                paramValue = @props.collection.sortDirection
                /></div>)
            key++
        if @props.collection.page
            params.push(<div key={key}> <QueryParam
                paramName = "Page"
                paramValue = @props.collection.page
                /></div>)
            key++
        if @props.collection.count
            params.push(<div key={key}> <QueryParam
                paramName = "Count"
                paramValue = @props.collection.count
                /></div>)
            key++
        return params

    renderPageButtons: ->
        #TODO

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
            <h1>Results Found</h1>
            <h2>Query Params</h2>
            {@getQueryParams()}
            <h2>Tasks</h2>
            {@renderPageButtons()}
            {@renderTasks()}
            {@renderPageButtons()}
        </div>

module.exports = DisplayResults