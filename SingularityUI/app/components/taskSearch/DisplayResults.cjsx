QueryParam = require './QueryParam'
Task = require './Task'

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
                paramValue = @props.collection.startedAfter
                /></div>)
            key++
        if @props.collection.startedBefore
            params.push(<div key={key}> <QueryParam
                paramName = "Started Before"
                paramValue = @props.collection.startedBefore
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

    getTasks: ->
        tasks = []
        key = 0
        for task in @props.collection.models
            tasks.push(<div key={key}><Task 
                task = task
                /></div>)
            key++
        return tasks


    render: ->
        return <div>
            <h1>Results Found</h1>
            <h2>Query Params</h2>
            {@getQueryParams()}
            <h2>Tasks</h2>
            {@getTasks()}
        </div>

module.exports = DisplayResults