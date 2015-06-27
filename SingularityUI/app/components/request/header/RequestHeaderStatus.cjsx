Helpers = require '../../utils/helpers'

RequestHeaderStatus = React.createClass

  displayName: 'RequestHeaderStatus'

  # propTypes:

  render: ->

    id = @props.data.request.id
    state = @props.data.request.state
    stateHumanized = Helpers.humanizeText @props.data.request.state
    typeHumanized = Helpers.humanizeText @props.data.request.type

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
