RequestHeader = require './header/RequestHeader'
RequestRunningInstances = require './RequestRunningInstances'
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
      <RequestRunningInstances />
      <RequestTaskHistory />
      <RequestDeployHistory />
      <RequestHistory />
    </div>

module.exports = RequestMain