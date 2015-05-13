Row = ReactBootstrap.Row
Col = ReactBootstrap.Col

SectionHeader = require '../lib/SectionHeader'
AdminTable = require './AdminTable'

ItemSection = React.createClass
  
  displayName: 'ItemSection'

  render: ->
    <Row id={@props.state}>
      <Col md={12}>
        <SectionHeader title={@props.title} />
        <AdminTable 
          state={@props.state}
          items={@props.items} 
          actions={@props.actions}
        />
      </Col>
    </Row>

module.exports = ItemSection