RequestHeader = require './header/RequestHeader'
RequestTaskHistory = require './RequestTaskHistory'
RequestDeployHistory = require './RequestDeployHistory'
RequestHistory = require './RequestHistory'
TableBase = require '../lib/TableBase'

## tables
RequestTasksActiveTable = require './RequestTasksActiveTable'


RequestMain = React.createClass

  displayName: 'RequestMain'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions:  React.PropTypes.func.isRequired

  render: ->
    <div>
      <RequestHeader
        data={@props.data}
        actions={@props.actions}
      />

      <TableBase
        headline="Running instances"
        data={@props.data.activeTasks}
        actions={@props.actions}
        table={RequestTasksActiveTable}
      />

      <TableBase
        headline="Task history"
        data={@props.data.taskHistory}
        actions={@props.actions}
        table={RequestTaskHistory}
      />

      <RequestDeployHistory />
      <RequestHistory />
    </div>

module.exports = RequestMain
