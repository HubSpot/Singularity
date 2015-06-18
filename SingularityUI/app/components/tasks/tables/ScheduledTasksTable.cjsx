ScheduledTasksTable = React.createClass

  displayName: 'ScheduledTasksTable'

  propTypes:
    tasks: React.PropTypes.array.isRequired
    # actions: React.PropTypes.func.isRequired
  
  componentWillMount: ->
    console.log 'ScheduledTasksTable will mount'

  render: ->
    <div>
      ScheduledTasksTable
    </div>

module.exports = ScheduledTasksTable