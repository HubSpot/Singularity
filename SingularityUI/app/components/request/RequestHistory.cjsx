SectionHeader = require '../lib/SectionHeader'

RequestHistory = React.createClass

  displayName: 'RequestHistory'

  # propTypes:

  render: ->
    <div>
      <SectionHeader title='Request History' />
    </div>

module.exports = RequestHistory