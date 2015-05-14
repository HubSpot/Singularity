Row = ReactBootstrap.Row
Col = ReactBootstrap.Col

FilterSearch = React.createClass
  
  displayName: 'FilterSearch'

  render: ->
    <Row>
      <Col md={12}>
        <input type="search" className="big-search-box" placeholder="Filter tasks" required />
      </Col>
    </Row>

module.exports = FilterSearch
