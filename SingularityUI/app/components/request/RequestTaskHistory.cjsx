SectionHeader = require '../lib/SectionHeader'
RequestTaskHistory = React.createClass

  displayName: 'RequestTaskHistory'

  # propTypes:

  render: ->
    <div className='page-section'>
      <SectionHeader title='Task History' />
    </div>

module.exports = RequestTaskHistory
