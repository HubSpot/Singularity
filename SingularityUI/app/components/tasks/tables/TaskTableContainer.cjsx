## Container for Tasks Tables

Helpers = require '../../utils/helpers'  
Label = ReactBootstrap.Label

TaskTableContainer = (Component) ->

  Table = React.createClass

    componentDidMount: ->
      @attachScrollListener()

    componentDidUpdate: ->
      @attachScrollListener()

    componentWillUnmount: ->
      @detachScrollListener()

    attachScrollListener: ->
      window.addEventListener 'scroll', @scrollListener
      window.addEventListener 'resize', @scrollListener
      if @props.tasks.length > 0
        @scrollListener()

    detachScrollListener: ->
      window.removeEventListener 'scroll', @scrollListener
      window.removeEventListener 'resize', @scrollListener

    scrollListener: ->
      if (window.innerHeight + window.scrollY) >= document.body.offsetHeight
        @props.renderMore()

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