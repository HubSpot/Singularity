Glyphicon = ReactBootstrap.Glyphicon

AdminTable =
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
  ## Table Rendering Helpers
  ##
  isTableEmpty: ->
    @props.items.length is 0

  generateLink: (item) ->
    "#{config.appRoot}/tasks/active/#{item.host}/"

  getStateBy: ->
    if @props.state is 'decommmisson'
      return <th>Decommissioned by</th> 
    if @props.state is 'active'
      return <th>Activated by</th>

  getUsername: (item) ->
    if @props.state is 'decommmisson' or @props.state is 'active'
      return <td>{ item.user }</td>

  getActionButtons: (item) ->
    if @props.state is 'active'
      return ( 
        <a data-item-host={ item.host } onClick={@handleChange} data-item-id={ item.id } data-state={ item.state } data-action="decommission" title="Decommission">
          <Glyphicon glyph='trash' />
        </a>
      )
    if @props.state is 'decommission'
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

    if @props.state is 'inactive'
      return (
        <a data-item-host={ item.host } onClick={@handleChange} data-item-id={ item.id } data-state={ item.state } data-action="remove" title="Remove">
          <Glyphicon glyph='remove' />
        </a>
      )



module.exports = AdminTable