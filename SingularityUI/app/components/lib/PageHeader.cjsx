Row = ReactBootstrap.Row
Col = ReactBootstrap.Col

PageHeader = React.createClass
  
  displayName: 'PageHeader'

  render: ->
    <Row>
      <Col md={12}>
        <div className="page-header page-header-noborder">
          <h1>{@props.title}</h1>
        </div>
      </Col>
    </Row>

module.exports = PageHeader

