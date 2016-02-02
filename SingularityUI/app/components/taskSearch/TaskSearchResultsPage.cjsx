Utils = require '../../utils'
TaskSearchResults = require '../../collections/TaskSearchResults'

TaskSearchResultsPage = React.createClass

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
			<div>
				<h1>Loading Results</h1>
				<p>{@props.requestId}</p>
			</div>
		else
			tasks = []
			i = 0
			for task in @collection.models
				tasks.push(<div key={i}>{task.taskId.id}<br /></div>)
				i++
			return <div>
				<h1>Results Found</h1>
				{tasks}
			</div>


module.exports = TaskSearchResultsPage