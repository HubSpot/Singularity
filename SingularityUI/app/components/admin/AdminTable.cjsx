Helpers = require '../helpers'  
EmptyTableMsg = require '../lib/EmptyTableMsg'
Table = ReactBootstrap.Table
Glyphicon = ReactBootstrap.Glyphicon


AdminTable = React.createClass
  
  displayName: 'AdminTable'

  ##
  ## Default State/Props
  ##
  propTypes:
    state: React.PropTypes.string.isRequired
    items: React.PropTypes.array.isRequired
    actions:  React.PropTypes.func.isRequired

  ##
  ## Event Handlers
  ##
  handleChange: (e) ->
    $target = $(e.currentTarget)
    id = $target.data('item-id')
    state = $target.data('state')
    host = $target.data('item-host')
    action = $target.data('action')

    @props.actions().changeItemState
      id: id
      state: state
      host: host
      action: action

  ##
  ## Build Table
  ##
  render: ->
    items = @props.items
    state = @props.state

    if items.length is 0
      return <EmptyTableMsg msg='No items' />

    getStateBy = ->
      if state is 'decommmisson'
        return <th>Decommissioned by</th> 
      if state is 'active'
        return <th>Activated by</th>

    tbody = items.map (item) =>

      link = "#{config.appRoot}/tasks/active/#{item.host}/"
      
      getUsername = ->
        if state is 'decommmisson' or state is 'active'
          return <td>{ item.user }</td>

      getActionButtons = ->
        if state is 'active'
          return ( 
            <a data-item-host={ item.host } onClick={@handleChange} data-item-id={ item.id } data-state={ item.state } data-action="decommission" title="Decommission">
              <Glyphicon glyph='trash' />
            </a>
          )
        if state is 'decommission'
          return (
            <span>
              <a data-item-host={ item.host } onClick={@handleChange} data-item-id={ item.id } data-state={ item.state } data-action="reactivate" title="Reactivate">
                <Glyphicon glyph='new-window' />
              </a>
              <a data-item-host={ item.host } onClick={@handleChange} data-item-id={ item.id } data-state={ item.state } data-action="remove" title="Remove">
                <Glyphicon glyph='remove' />
              </a>
            </span>
          )

        if state is 'inactive'
          return (
            <a data-item-host={ item.host } onClick={@handleChange} data-item-id={ item.id } data-state={ item.state } data-action="remove" title="Remove">
              <Glyphicon glyph='remove' />
            </a>
          )

      return(
        <tr key={item.id}>
          <td>
              <a href={link}>{ item.id }</a>
          </td>
          <td>{ item.state }</td>
          <td>{ Helpers.timestampFormatted item.currentState.timestamp }</td>
          <td>{ item.rackId }</td>
          <td>{ item.host }</td>
          <td className="hidden-xs" data-value="{ item.uptime }">
              { Helpers.timestampDuration item.uptime }
          </td>
          { getUsername() }
          <td className="actions-column">
            { getActionButtons.call @ }
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
            { getStateBy() }
            <th data-sortable="false"></th>
          </tr>
        </thead>
        <tbody>
          {tbody}
        </tbody>
      </Table>
    )

module.exports = AdminTable