SectionHeader = require '../lib/SectionHeader'
EmptyTableMsg  = require '../lib/EmptyTableMsg'

RequestRunningInstances = React.createClass

  displayName: 'RequestRunningInstances'

  # propTypes:

  render: ->
    <div>
      <SectionHeader title='Running Instances' />
      <EmptyTableMsg msg='No active tasks' />
    </div>

module.exports = RequestRunningInstances