Row = ReactBootstrap.Row
Col = ReactBootstrap.Col

SectionHeader = require '../lib/SectionHeader'
StarredRequestsTable   = require './StarredRequestsTable'
requestsTotal = require './RequestsTotal'

Requests = React.createClass
  
  displayName: 'requests'

  render: ->

    boxes = @props.data.totals.map (item) =>
      return(
        <Col md={2} key={item.label}>
          <requestsTotal 
            item=item 
          /> 
        </Col>
      )
    
    return(
      <div>
        <Row>
          <Col md={12}>
            <SectionHeader title='My requests' />
          </Col>
        </Row>
        <Row>
          {boxes}
        </Row>
        <Row>
          <Col md={12}>
            <SectionHeader title='Starred requests' />
            <StarredRequestsTable
              data={@props.data}
              unstar={@props.actions().unstar}
              sortStarredRequests={@props.actions().sortStarredRequests}
            />
          </Col>
        </Row>
      </div>
    )

module.exports = Requests