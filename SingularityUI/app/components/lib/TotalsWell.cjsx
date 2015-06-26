TotalsWell = React.createClass

  displayName: 'TotalsWell'

  render: ->
    return (
      <a className="big-number-link" href={@props.link}>
          <div className="well">
            <div className="big-number">
                <div className="number">
                  {@props.number}
                </div>
                <div className="number-label">{@props.label}</div>
            </div>
        </div>
      </a>
    )

module.exports = TotalsWell