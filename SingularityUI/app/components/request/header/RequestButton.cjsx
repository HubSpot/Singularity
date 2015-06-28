Button = ReactBootstrap.Button

RequestButton = React.createClass

  displayName: 'RequestButton'

  propTypes:
    bsStyle: React.PropTypes.string
    action: React.PropTypes.string.isRequired

  getDefaultProps: ->
    { bsStyle: 'default' }

  render: ->
    <Button bsStyle={@props.bsStyle} data-action={@props.action}>
      {@props.children}
    </Button> 

module.exports = RequestButton