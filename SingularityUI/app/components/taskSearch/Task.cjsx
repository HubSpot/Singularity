Task = React.createClass

    render: ->
        <p>{@props.task.attributes.taskId.id} {@props.task.attributes.taskId.startedAt}</p>

module.exports = Task