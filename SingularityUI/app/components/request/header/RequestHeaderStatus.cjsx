Helpers = require '../../utils/helpers'

RequestHeaderStatus = React.createClass

  displayName: 'RequestHeaderStatus'

  propTypes:
    state: React.PropTypes.string.isRequired
    id: React.PropTypes.string.isRequired
    type: React.PropTypes.string.isRequired

  render: ->
    id = @props.id
    state = @props.state
    stateHumanized = Helpers.humanizeText @props.state
    typeHumanized = Helpers.humanizeText @props.type

    return (
      <div>
        <h4>
          <span className="request-state" data-state={state}>
              {stateHumanized}
          </span>
          <span className="request-type">
              {typeHumanized}
          </span>
        </h4>    
        <h2>
          {id}
        </h2> 
      </div>
    )

module.exports = RequestHeaderStatus