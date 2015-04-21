SectionHeader = require '../lib/sectionHeader'
requestsTotal = require './requestsTotal'

Requests = React.createClass

  componentDidMount: ->    
    # test collection event bindings:
    # setTimeout ( =>
    #   @props.requestsTotals
    # ), 1500

  render: ->
    boxes = @props.requestTotals.map (item) =>
      return(
        <requestsTotal 
          key=item.label 
          item=item 
        /> 
      )
      
    return(
      <section>
          <SectionHeader title='My Requests' />
          <div className="row">
              {boxes}
          </div>
      </section>
    )

module.exports = Requests