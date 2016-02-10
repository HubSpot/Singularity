TaskSearchResults = require '../../collections/TaskSearchResults'

QueryParam = require './QueryParam'
Task = require './Task'


StripedTable = require '../common/Table'
TimeStamp = require '../common/TimeStamp'
TaskStateLabel = require '../common/TaskStateLabel'
Link = require '../common/Link'
FormField = require '../common/input/FormField'
DropDown = require '../common/input/DropDown'
Header = require './Header'
Enums = require './Enums'

DisplayResults = React.createClass

    collectionsReset: (event, response) ->
        @setState({
            loading: false
        })

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

    fetchCollection: ->
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
        if @collection.params.requestId
            params.push(<div key={key}> <QueryParam
                paramName = "Request Id"
                paramValue = @props.requestId
                onClick = @props.clearRequestId
                cantClear = {not @props.global}
                /></div>)
            key++
        if @collection.params.deployId
            params.push(<div key={key}> <QueryParam
                paramName = "Deploy Id"
                paramValue = @props.deployId
                onClick = @props.clearDeployId
                /></div>)
            key++
        if @collection.params.host
            params.push(<div key={key}> <QueryParam
                paramName = "Host"
                paramValue = @props.host
                onClick = @props.clearHost
                /></div>)
            key++
        if @collection.params.lastTaskStatus
            params.push(<div key={key}> <QueryParam
                paramName = "Last Task Status"
                paramValue = @props.lastTaskStatus
                onClick = @props.clearLastTaskStatus
                /></div>)
            key++
        if @collection.params.startedAfter
            params.push(<div key={key}> <QueryParam
                paramName = "Started After"
                paramValue = @props.startedAfter._d.toString()
                onClick = @props.clearStartedAfter
                /></div>)
            key++
        if @collection.params.startedBefore
            params.push(<div key={key}> <QueryParam
                paramName = "Started Before"
                paramValue = @props.startedBefore._d.toString()
                onClick = @props.clearStartedBefore
                /></div>)
            key++
        if @collection.params.sortDirection
            params.push(<div key={key}> <QueryParam
                paramName = "Sort Direction"
                paramValue = @props.sortDirection
                onClick = @props.clearSortDirection
                /></div>)
            key++
        return params

    updatePageNumber: (event) ->
        @setState({
            pageNumberEntered: event.target.value
        })

    handlePageJump: (event) ->
        event.preventDefault()
        @props.setPageNumber(@state.pageNumberEntered)
        @setState({
            pageNumberEntered: ''
        })

    updateCount: (event) ->
        @props.updateCount(event.target.value)

    # using className="previous" for the next button is necessary to align
    # it to the left side of the page. This is built into bootstrap
    renderPageToggles: ->
        <div className="container-fluid">
            <nav>
                <ul className="pager line">
                    <li className={@collection.params.page == 1 and "previous disabled" or "previous"} onClick={@props.decreasePageNumber}><a href="#">Previous</a></li>
                    <li className="previous disabled"><a href="#">Page {@collection.params.page}</a></li>
                    <li className="previous" onClick={@props.increasePageNumber}><a href="#">Next</a></li>
                    <form role="form" onSubmit={@handlePageJump} className='form-inline text-left'>
                        <div className='form-group'>
                            <label htmlFor="pageNumber" className="sr-only">Jump To Page:</label>
                            <FormField 
                                value = @state.pageNumberEntered 
                                inputType = 'number'
                                id = 'pageNumber'
                                title = "Jump to Page"
                                updateFn = @updatePageNumber />
                        </div>
                        <button type="submit" className="btn btn-default">Jump!</button>
                        &nbsp;&nbsp;
                        <div className='form-group'>
                            <label htmlFor='count'>Tasks Per Page: </label>
                            &nbsp;&nbsp;
                            <DropDown
                                forceChooseValue = true
                                value = @props.count
                                choices = {@props.countChoices}
                                inputType = 'number'
                                id = 'count'
                                title = 'Tasks Per Page'
                                updateFn = @updateCount />
                        </div>
                        &nbsp;&nbsp;
                        <div className='form-group'>
                            <label htmlFor="sortDirection">Sort Direction:</label>
                            &nbsp;&nbsp;
                            <DropDown
                                forceChooseValue = true
                                value = @props.sortDirection
                                choices = Enums.sortDirections()
                                inputType = 'sortDirection'
                                id = 'sortDirection'
                                title = 'Sort Direction'
                                updateFn = @props.updateSortDirection />
                        </div>
                    </form>
                </ul>
            </nav>
        </div>


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
            <Header
                global = @props.global
                requestId = @props.requestId
            />
            <h2>Query Parameters</h2>
            <div className="row">
                <div className="col-md-6">
                    <ul className="list-group">
                        {@getQueryParams()}
                    </ul>
                </div>
            </div>
            <button className="btn btn-primary" onClick={@props.returnToForm}>Modify Query Parameters</button>
            <h2>Tasks</h2>
            {@renderPageToggles()}
            {@renderTasks()}
            {@renderPageToggles()}
        </div>

module.exports = DisplayResults