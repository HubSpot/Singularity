RequestHeaderStatus = require './RequestHeaderStatus'
RequestHeaderButtons = require './RequestHeaderButtons'
RequestHeaderDeploy = require './RequestHeaderDeploy'

Row = ReactBootstrap.Row
Col = ReactBootstrap.Col


RequestHeader = React.createClass

  displayName: 'RequestHeader'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions: React.PropTypes.func.isRequired

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
        <Col md={4} className='button-container'>
          <RequestHeaderButtons
            data={@props.data}
            refresh={@props.actions().refresh}
            viewRequestJSON={@props.actions().viewRequestJSON}
            callModelMethod={@props.actions().callModelMethod}
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
