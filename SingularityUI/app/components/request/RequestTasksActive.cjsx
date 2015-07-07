SectionHeader = require '../lib/SectionHeader'
EmptyTableMsg  = require '../lib/EmptyTableMsg'
RequestTasksActiveTable = require './RequestTasksActiveTable'

RequestRunningInstances = React.createClass

  displayName: 'RequestRunningInstances'

  propTypes:
    activeTasks: React.PropTypes.array.isRequired

  render: ->
    if @props.activeTasks.length is 0
      activeTasks = <EmptyTableMsg msg='No active tasks' />
    else
      activeTasks =
        <RequestTasksActiveTable
          activeTasks={@props.activeTasks}
          getModel={@props.actions().getModel}
        />

    <div className='page-section'>
      <SectionHeader title='Running Instances' />
      {activeTasks}
    </div>

module.exports = RequestRunningInstances
