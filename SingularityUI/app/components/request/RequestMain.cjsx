RequestHeader = require './RequestHeader'
RequestRunningInstances = require './RequestRunningInstances'
RequestTaskHistory = require './RequestTaskHistory'
RequestDeployHistory = require './RequestDeployHistory'
RequestHistory = require './RequestHistory'


RequestMain = React.createClass

  displayName: 'RequestMain'

  # propTypes:

  render: ->
    <div>
      <RequestHeader
        data={@props.data}
      />
      <RequestRunningInstances />
      <RequestTaskHistory />
      <RequestDeployHistory />
      <RequestHistory />
    </div>

module.exports = RequestMain