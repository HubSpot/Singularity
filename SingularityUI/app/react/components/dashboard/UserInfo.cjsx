UserInfo = React.createClass

  propTypes:
      user: React.PropTypes.instanceOf(Backbone.Model).isRequired

  handleChangeUser: ->
    app.deployUserPrompt()

  componentDidMount: ->
    @props.user.on 'change', @props.refresh

  render: ->
    <header>
        <h1>{@props.user.get('deployUser')} <small><a onClick={this.handleChangeUser} data-action="change-user">change</a></small></h1>
    </header>

module.exports = UserInfo