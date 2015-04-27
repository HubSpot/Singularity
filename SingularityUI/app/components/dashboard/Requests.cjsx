SectionHeader = require '../lib/SectionHeader'
StarredRequestsTable   = require './StarredRequestsTable'
requestsTotal = require './RequestsTotal'

Requests = React.createClass
  
  displayName: 'requests'

  render: ->
    boxes = @props.requestTotals.map (item) =>
      return(
        <requestsTotal 
          key=item.label 
          item=item 
        /> 
      )
      
    return(
      <div>
          <div className="row">
              <div className="col-md-12">
                <SectionHeader title='My requests' />
              </div>
          </div>
          <div className="row">
              {boxes}
          </div>
          <div className="row">
             <div className="col-md-12">
                <SectionHeader title='Starred requests' />
                <StarredRequestsTable
                  starredRequests={@props.starredRequests}
                  unstar={@props.unstar}
                  sortStarredRequests={@props.sortStarredRequests}
                  sortedAsc={@props.sortedAsc}
                />
            </div>
          </div>
      </div>
    )

module.exports = Requests