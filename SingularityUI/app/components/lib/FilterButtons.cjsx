PillsNav = require '../lib/PillsNav'
NavItem = ReactBootstrap.NavItem
Helpers = require '../utils/helpers'

FilterButtons = React.createClass

  displayName: 'FilterButtons'

  propTypes:
    data: React.PropTypes.object.isRequired
    buttons: React.PropTypes.array.isRequired
    changeTable: React.PropTypes.func.isRequired

  handleButtonClick: (e) ->
    filter = e.currentTarget.getAttribute('id')
    target = e.currentTarget.getAttribute('href')
    Helpers.routeComponentLink(e, target)

    @setState { activeFilter: filter}
    @props.changeTable filter

  getInitialState: ->
    { activeFilter: @props.data.initialFilterState }

  render: ->
    buttons = @props.buttons.map (button) =>
      currentFilter = @state.activeFilter || @props.data.initialFilterState
      classes = 'active' if currentFilter is button.id

      return(
        <li key={button.label} className={classes}>
          <a href={button.link} onClick={@handleButtonClick} id={button.id}>{button.label}</a>
        </li>
      )

    <PillsNav> 
      {buttons} 
    </PillsNav>

module.exports = FilterButtons
