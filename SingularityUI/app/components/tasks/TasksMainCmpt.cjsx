FilterButtons = require '../lib/FilterButtons'
FilterSearch = require '../lib/FilterSearch'
TasksTable = require './TasksTable'

TasksMain = React.createClass

  displayName: 'TasksMain'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions: React.PropTypes.func.isRequired
  
  
  render: ->

    <div>
      <FilterButtons
        md={12}
        data={@props.data}
        changeFilterState={@props.actions().changeFilterState}
        buttons={[
          { label: 'Active', id: 'active', nolink: true }
          { label: 'Scheduled', id: 'scheduled' }
          { label: 'Cleaning', id: 'cleaning' }
          { label: 'LB Cleaning', id: 'lbcleanup' }
          { label: 'Decommissioning', id: 'decommissioning' }
        ]}
      />

      <FilterSearch />
      <TasksTable 
        data={@props.data}
        actions={@props.actions}
      />
    </div>

module.exports = TasksMain