## Container for Tasks Tables

Helpers = require '../../utils/helpers'  
Label = ReactBootstrap.Label

TaskTableContainer = (Component) ->

  Table = React.createClass

    pendingTask: (task) ->
      if task.pendingTask?
        if Helpers.isTimestampInPast task.pendingTask.pendingTaskId.nextRunAt
          <Label bsStyle='danger'>OVERDUE</Label>

    render: ->
      <Component 
        {...@props} 
        {...@state} 
        pendingTask={@pendingTask}
      />

module.exports = TaskTableContainer