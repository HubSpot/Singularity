TaskSearchResults = require '../../collections/TaskSearchResults'

QueryParameters = require '../common/QueryParameters'
FormField = require '../common/atomicFormItems/FormField'
DropDown = require '../common/atomicFormItems/DropDown'
TaskTable = require '../common/TaskTable'

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
        [
            {
                show: @collection.params.requestId
                name: "Request Id"
                value: @props.requestId
                clearFn: @props.clearRequestId
                cantClear: not @props.global
            },
            {
                show: @collection.params.deployId
                name: "Deploy Id"
                value: @props.deployId
                clearFn: @props.clearDeployId
            },
            {
                show: @collection.params.host
                name: "Host"
                value: @props.host
                clearFn: @props.clearHost
            },
            {
                show: @collection.params.lastTaskStatus
                name: "Last Task Status"
                value: @props.lastTaskStatus
                clearFn: @props.clearLastTaskStatus
            },
            {
                show: @collection.params.startedBefore
                name: "Started Before"
                value: @props.startedBefore.format window.config.timestampWithSecondsFormat if @props.startedBefore
                clearFn: @props.clearStartedBefore
            },
            {
                show: @collection.params.startedAfter
                name: "Started After"
                value: @props.startedAfter.format window.config.timestampWithSecondsFormat if @props.startedAfter
                clearFn: @props.clearStartedAfter
            }
        ]

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
                    <li className={classNames 'previous', {'disabled': @collection.params.page == 1}} onClick={@props.decreasePageNumber}><a href="#">Previous</a></li>
                    <li className="previous disabled"><a href="#">Page {@collection.params.page}</a></li>
                    <li className="previous" onClick={@props.increasePageNumber}><a href="#">Next</a></li>
                    <form role="form" onSubmit={@handlePageJump} className='form-inline text-left'>
                        <div className='form-group'>
                            <label htmlFor="pageNumber" className="sr-only">Jump To Page:</label>
                            <FormField 
                                id = 'pageNumber'
                                prop = {{
                                    value: @state.pageNumberEntered 
                                    inputType: 'number'
                                    updateFn: @updatePageNumber
                                }} />
                        </div>
                        <button type="submit" className="btn btn-default">Jump!</button>
                        &nbsp;&nbsp;
                        <div className='form-group'>
                            <label htmlFor='count'>Tasks Per Page: </label>
                            &nbsp;&nbsp;
                            <DropDown
                                id = 'count'
                                prop = {{
                                    forceChooseValue: true
                                    value: @props.count
                                    choices: @props.countChoices
                                    inputType: 'number'
                                    updateFn: @updateCount
                                }} />
                        </div>
                        &nbsp;&nbsp;
                        <div className='form-group'>
                            <label htmlFor="sortDirection">Sort Direction:</label>
                            &nbsp;&nbsp;
                            <DropDown
                                id = 'sortDirection'
                                prop = {{
                                    forceChooseValue: true
                                    value: @props.sortDirection
                                    choices: Enums.sortDirections()
                                    inputType: 'sortDirection'
                                    updateFn: @props.updateSortDirection
                                }} />
                        </div>
                    </form>
                </ul>
            </nav>
        </div>


    render: ->
        @fetchCollection() if @willFetch
        <div>
            <Header
                global = @props.global
                requestId = @props.requestId
            />
            <h2>Query Parameters</h2>
            <QueryParameters
                colSize = "md-6"
                parameters = {@getQueryParams()}
            />
            <button className="btn btn-primary" onClick={@props.returnToForm}>Modify Query Parameters</button>
            <h2>Tasks</h2>
            {@renderPageToggles()}
            <TaskTable
                models = {@collection.models}
            />
            {@renderPageToggles()}
        </div>

module.exports = DisplayResults
