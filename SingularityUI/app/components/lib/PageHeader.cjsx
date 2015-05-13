Row = ReactBootstrap.Row
Col = ReactBootstrap.Col

Helpers = require '../helpers'


PageHeader = React.createClass
  
  displayName: 'PageHeader'

  render: ->
    title = @props.title
    if @props.titleCase
      title = Helpers.titleCase title

    return(
      <Row>
        <Col md={12}>
          <div className="page-header page-header-noborder">
            <h1>{title}</h1>
          </div>
        </Col>
      </Row>
    )

module.exports = PageHeader

