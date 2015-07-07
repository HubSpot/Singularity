Row = ReactBootstrap.Row
Col = ReactBootstrap.Col

SectionHeader = require '../lib/SectionHeader'
StarredRequestsTable   = require './StarredRequestsTable'

StarredRequests = React.createClass
  
  displayName: 'StarredRequests'

  render: ->
    <Row>
      <Col md={12}>
        <SectionHeader title='Starred requests' />
        <StarredRequestsTable
          data={@props.data}
          unstar={@props.actions().unstar}
          sortTable={@props.actions().sortTable}
        />
      </Col>
    </Row>

module.exports = StarredRequests