Glyphicon = ReactBootstrap.Glyphicon

StarItem = React.createClass
  
  displayName: 'StarItem'

  propTypes:
    id: React.PropTypes.string.isRequired
    clickEvent: React.PropTypes.func
    active: React.PropTypes.string

  render: ->
    <a className="star" onClick={@props.clickEvent} data-id={@props.id} data-starred={@props.active}>
        <Glyphicon glyph='star' />
    </a>

module.exports = StarItem