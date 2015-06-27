
RequestHeaderStatus = React.createClass

  displayName: 'RequestHeaderStatus'

  # propTypes:

  render: ->
    <div>
      <h4>
        <span className="request-state" data-state=" data.state ">
            humanizeText data.state
        </span>
        <span className="request-type">
            humanizeText data.type
        </span>
      </h4>    

      <h2>
           data.id 
      </h2> 

    </div>

module.exports = RequestHeaderStatus
