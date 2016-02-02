DisplayResults = React.createClass

	render: ->
		tasks = []
		i = 0
		for task in @props.collection.models
			tasks.push(<div key={i}>{task.taskId.id}<br /></div>)
			i++
		return <div>
			<h1>Results Found</h1>
			{tasks}
		</div>

module.exports = DisplayResults