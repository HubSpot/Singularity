SectionHeader = require '../lib/SectionHeader'
StarredRequestsTable   = require './starredRequestsTable'

requestsTotal = require './requestsTotal'

Requests = React.createClass

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
          <SectionHeader title='My requests' />
          <div className="row">
              {boxes}
          </div>
          <SectionHeader title='Starred requests' />
          <StarredRequestsTable
            starredRequests={@props.starredRequests}
          />
      </section>
    )

module.exports = Requests