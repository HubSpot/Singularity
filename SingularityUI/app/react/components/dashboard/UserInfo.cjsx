UserInfo = React.createClass
  
  displayName: 'userInfo'

  handleChangeUser: ->
    app.deployUserPrompt()

  render: ->
    <div className='row'>
      <div className='col-md-12'>
        <header>
            <h1>{@props.user.get('deployUser')} <small><a onClick={this.handleChangeUser} data-action="change-user">change</a></small></h1>
        </header>
      </div>
    </div>

module.exports = UserInfo