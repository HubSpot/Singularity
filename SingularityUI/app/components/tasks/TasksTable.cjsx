Helpers = require '../helpers'

Tables =
  'active'    : require './tables/ActiveTasksTable'
  'scheduled' : require './tables/ScheduledTasksTable'
  'cleaning'  : require './tables/CleaningTasksTable'
  'lbcleanup' : require './tables/LBCleanupTasksTable'
  'decommissioning' : require './tables/DecommTasksTable'

TasksTable = React.createClass

  displayName: 'TasksTable'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions: React.PropTypes.func.isRequired

  componentDidMount: ->
    console.log 'componentDidMount'
  
  componentWillUnmount: ->
    console.log 'componentWillMount'

  render: ->
    filter = @props.data.filterState || @props.data.initialFilterState
    table = Tables[filter]
    React.createElement(table, {tasks: @props.data.tasks} )

module.exports = TasksTable