Glyphicon   = ReactBootstrap.Glyphicon
Symbol      = require './Symbol'

TableRowAction = React.createClass
  
  displayName: 'Table Row Action'

  propTypes:
    id: React.PropTypes.string.isRequired
    action: React.PropTypes.string.isRequired
    title: React.PropTypes.string
    markup: React.PropTypes.string
    symbol: React.PropTypes.string

  render: ->
    <a data-task-id={ @props.id } data-action={@props.action} title={@props.title}>
        {if @props.glyph then <Glyphicon glyph={@props.glyph} />}
        {if @props.text then @props.text}
        {if @props.symbol then <Symbol type={@props.symbol} />}
    </a>

module.exports = TableRowAction