Utils = require '../../utils'
TaskSearchResults = require '../../collections/TaskSearchResults'

TaskSearchResultsPage = React.createClass

	###componentWillMount: ->
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
		@results = @collection.fetch().done()
		#debugger###

	render: ->
		<div>
			<h1>Results</h1>
		</div>

module.exports = TaskSearchResultsPage