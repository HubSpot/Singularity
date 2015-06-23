PillsNav = require '../lib/PillsNav'
NavItem = ReactBootstrap.NavItem
Helpers = require '../utils/helpers'

FilterButtons = React.createClass

  displayName: 'FilterButtons'

  propTypes:
    data: React.PropTypes.object.isRequired
    buttons: React.PropTypes.array.isRequired

  getInitialState: ->
    { activeFilter: @props.data.initialFilterState }

  render: ->
    buttons = @props.buttons.map (button) =>
      link = "#{window.config.appRoot}/tasks/#{button.id}"
      classes = 'active' if @state.activeFilter is button.id

      return(
        <NavItem key={button.label} id={button.id} href={link} className={classes} onClick={Helpers.routeLink}>
          {button.label}
        </NavItem>
      )

    <PillsNav> 
      {buttons} 
    </PillsNav>

module.exports = FilterButtons
