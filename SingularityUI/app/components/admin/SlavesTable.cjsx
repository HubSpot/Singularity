AdminTable = require './AdminTable'

Helpers       = require '../utils/helpers'  
Table         = ReactBootstrap.Table
EmptyTableMsg = require '../lib/EmptyTableMsg'

SlavesTable = React.createClass
  
  displayName: 'Slaves Table'

  render: ->
  
    if @props.isTableEmpty()
      return <EmptyTableMsg msg='No items' />

    tbody = @props.items.map (item) =>
      
      return(
        <tr key={item.id}>
          <td>
            <a href={@props.generateLink(item)}>{ item.id }</a>
          </td> 
          <td>{ item.state }</td>
          <td>{ Helpers.timestampFormatted item.currentState.timestamp }</td>
          <td>{ item.rackId }</td>
          <td>{ item.host }</td>
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
            <th>Since</th>
            <th>Rack</th>
            <th>Host</th>
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

buildSlavesTable = AdminTable(SlavesTable)
module.exports = buildSlavesTable