Helpers       = require '../utils/helpers'  
Table         = ReactBootstrap.Table
EmptyTableMsg = require '../lib/EmptyTableMsg'
AdminTable    = require '../utils/mixins/AdminTable'

SlavesTable = React.createClass
  
  displayName: 'Slaves Table'
  mixins: [AdminTable]

  ##
  ## Build Table
  ##
  render: ->
  
    if @isTableEmpty()
      return <EmptyTableMsg msg='No items' />

    tbody = @props.items.map (item) =>
      
      return(
        <tr key={item.id}>
          <td>
            <a href={@generateLink(item)}>{ item.id }</a>
          </td> 
          <td>{ item.state }</td>
          <td>{ Helpers.timestampFormatted item.currentState.timestamp }</td>
          <td>{ item.rackId }</td>
          <td>{ item.host }</td>
          <td className="hidden-xs" data-value="{ item.uptime }">
              { Helpers.timestampDuration item.uptime }
          </td>
          { @getUsername(item) }
          <td className="actions-column">
            { @getActionButtons(item) }
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
            { @getStateBy() }
            <th data-sortable="false"></th>
          </tr>
        </thead>
        <tbody>
          {tbody}
        </tbody>
      </Table>
    )

module.exports = SlavesTable