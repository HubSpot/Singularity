Helpers = require '../helpers'
ActiveTasksTable = require './tables/ActiveTasksTable'
ScheduledTasksTable = require  './tables/ScheduledTasksTable'

TasksTable = React.createClass

  displayName: 'TasksTable'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions: React.PropTypes.func.isRequired
  
  
  render: ->
    filter = @props.data.filterState || @props.data.initialFilterState
   
    # TableComponent = Helpers.titleCase(filter) + 'TasksTable'
    console.log 'filter: ', filter

    if filter is 'active'
      table = 
        <div>
          <ActiveTasksTable
            tasks={@props.data.tasks}
          />
        </div>

    return table

module.exports = TasksTable

