Helpers = require '../../utils/helpers'
Button = ReactBootstrap.Button

RequestButton = React.createClass

  displayName: 'RequestButton'

  propTypes:
    bsStyle: React.PropTypes.string
    buttonClick: React.PropTypes.func

  getDefaultProps: ->
    { bsStyle: 'default' }

  handleClick: (e) ->
    link = e.currentTarget.getAttribute('data-link')
    if link?
      return Helpers.routeComponentLink null, link, true
    @props.buttonClick e

  render: ->
    <Button className='button-whitespace' onClick={@handleClick} data-action={@props.action} data-id={@props.id} bsStyle={@props.bsStyle} data-link={@props.link}>
      {@props.children}
    </Button> 

module.exports = RequestButton