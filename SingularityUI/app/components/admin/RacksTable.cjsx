BuildAdminTable = require './BuildAdminTable'

Helpers        = require '../utils/helpers'  
Table          = ReactBootstrap.Table
EmptyTableMsg  = require '../lib/EmptyTableMsg'


RacksTable = React.createClass
  
  displayName: 'RacksTable'

  render: ->
    if @props.isTableEmpty()
      return <EmptyTableMsg msg='No items' />

    tbody = @props.items.map (item) =>
      
      return(
        <tr key={item.id}>
          <td>
            { item.id }
          </td> 
          <td>{ item.state }</td>
          <td className="hidden-xs" data-value="{ item.uptime }">
              { Helpers.timestampDuration item.uptime }
          </td>
          { @props.getUsername(item) }
          <td className="actions-column">
            { @props.getActionButtons(item) }
          </td>
        </tr>
      )

    return (
      <Table striped>
        <thead>
          <tr>
            <th>ID</th>
            <th>State</th>
            <th className="hidden-xs">Uptime</th>
            { @props.getStateBy() }
            <th data-sortable="false"></th>
          </tr>
        </thead>
        <tbody>
          {tbody}
        </tbody>
      </Table>
    )

buildRacksTable = BuildAdminTable(RacksTable)
module.exports = buildRacksTable