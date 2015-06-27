RequestHeaderStatus = require './header/RequestHeaderStatus'
RequestHeaderButtons = require './header/RequestHeaderButtons'
RequestHeaderDeploy = require './header/RequestHeaderDeploy'

Row = ReactBootstrap.Row
Col = ReactBootstrap.Col


RequestHeader = React.createClass

  displayName: 'RequestHeader'

  # propTypes:

  render: ->
    <header className='detail-header'>
      <Row>
        <Col md={8}>
          <RequestHeaderStatus />     
        </Col>
        <Col md={4}>
          <RequestHeaderButtons />
        </Col>
      </Row>
      <Row>
        <Col md={12}>
          <RequestHeaderDeploy />
        </Col>
      </Row>
    </header>

module.exports = RequestHeader