SectionHeader = require '../lib/SectionHeader'

RequestDeployHistory = React.createClass

  displayName: 'RequestDeployHistory'

  # propTypes:

  render: ->
    <div>
      <SectionHeader title='Deploy History' />
    </div>

module.exports = RequestDeployHistory