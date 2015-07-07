RequestHeader = require './header/RequestHeader'
RequestTasksActive = require './RequestTasksActive'
RequestTaskHistory = require './RequestTaskHistory'
RequestDeployHistory = require './RequestDeployHistory'
RequestHistory = require './RequestHistory'


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
      <RequestTasksActive
        activeTasks={@props.data.activeTasks}
        actions={@props.actions}
      />
      <RequestTaskHistory
        taskHistory={@props.data.taskHistory}
      />
      <RequestDeployHistory />
      <RequestHistory />
    </div>

module.exports = RequestMain
