SectionHeader = require '../lib/SectionHeader'
EmptyTableMsg  = require '../lib/EmptyTableMsg'

Table = React.createClass

  displayName: 'Table'

  propTypes:
    headline: React.PropTypes.string.isRequired

  render: ->
    if @props.data.length is 0
      message = "No #{@props.headline}"
      table = <EmptyTableMsg msg={message} />
    else
      tableProps =
        data: @props.data
        actions: @props.actions 

      table = React.createElement(@props.table, tableProps)

    <div className='page-section'>
      <SectionHeader title={@props.headline} />
      {table}
    </div>

module.exports = Table