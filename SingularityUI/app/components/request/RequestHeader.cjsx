RequestHeaderStatus = require './header/RequestHeaderStatus'
RequestHeaderButtons = require './header/RequestHeaderButtons'
RequestHeaderDeploy = require './header/RequestHeaderDeploy'

Row = ReactBootstrap.Row
Col = ReactBootstrap.Col


RequestHeader = React.createClass

  displayName: 'RequestHeader'

  propTypes:
    data: React.PropTypes.object.isRequired

  render: ->
    
    if @props.data.request.state is undefined
      return (<div></div>)

    <header className='detail-header'>
      <Row>
        <Col md={8}>
          <RequestHeaderStatus 
            id={@props.data.request.id}
            state={@props.data.request.state}
            type={@props.data.request.type}
          />     
        </Col>
        <Col md={4}>
          <RequestHeaderButtons 
            data={@props.data}
          />
        </Col>
      </Row>
      <Row>
        <Col md={12}>
          <RequestHeaderDeploy 
            data={@props.data}
          />
        </Col>
      </Row>
    </header>

module.exports = RequestHeader