Task = React.createClass

    render: ->
        <p>{@props.task.attributes.taskId.id}</p>

module.exports = Task