Helpers       = require '../helpers'  
Table         = ReactBootstrap.Table
EmptyTableMsg = require '../lib/EmptyTableMsg'
AdminTable    = require '../mixins/AdminTable'

RacksTable = React.createClass
  
  displayName: 'Racks Table'
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
            { item.id }
          </td> 
          <td>{ item.state }</td>
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

module.exports = RacksTable