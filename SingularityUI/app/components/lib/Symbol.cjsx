Symbol = React.createClass
  
  displayName: 'Symbol'

  propTypes:
    type: React.PropTypes.string.isRequired

  symbols:
    'code': '\u007B \u007D'


  render: ->
    symbol = @symbols[@props.type]
    return (
      <span> {symbol} </span>
    )

module.exports = Symbol