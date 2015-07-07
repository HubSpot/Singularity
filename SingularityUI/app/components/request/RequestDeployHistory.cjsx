SectionHeader = require '../lib/SectionHeader'

RequestDeployHistory = React.createClass

  displayName: 'RequestDeployHistory'

  # propTypes:

  render: ->
    <div className='page-section'>
      <SectionHeader title='Deploy History' />
    </div>

module.exports = RequestDeployHistory
