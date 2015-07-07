Row = ReactBootstrap.Row
Col = ReactBootstrap.Col


UserInfo = React.createClass
  
  displayName: 'UserInfo'

  handleChangeUser: ->
    app.deployUserPrompt()

  render: ->
    user = @props.data.user.get('deployUser')

    <Row>
      <Col md={12}>
        <div className="page-header page-header-noborder">
            <h1>{user} <small><a onClick={this.handleChangeUser} data-action="change-user">change</a></small></h1>
        </div>
      </Col>
    </Row>

module.exports = UserInfo