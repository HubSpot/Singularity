Row = ReactBootstrap.Row
Col = ReactBootstrap.Col

PillsNav = require '../lib/PillsNav'
NavItem = ReactBootstrap.NavItem


FilterButtons = React.createClass
  
  displayName: 'FilterButtons'
  
  propTypes:
    data: React.PropTypes.object.isRequired
    changeFilterState: React.PropTypes.func.isRequired
    buttons: React.PropTypes.array.isRequired
    md: React.PropTypes.number

  getInitialState: ->
    { activeFilter: @props.data.initialFilterState }
  
  handleFilterChange: (e) ->
    e.preventDefault()
    filter = e.currentTarget.getAttribute('id')    
    app.router.navigate "/tasks/#{filter}", { replace: true }
    @setState( activeFilter: filter)
    @props.changeFilterState filter

  render: ->
    
    buttons = @props.buttons.map (button) =>  
      link = "#{window.config.appRoot}/tasks/#{button.id}"
      classes = 'active' if @state.activeFilter is button.id
      
      return(
        <NavItem key={button.label} id={button.id} href={link} className={classes} onClick={@handleFilterChange}>
          {button.label}
        </NavItem>
      )

    if @props.md
      Nav = 
        <Row>
          <Col md={@props.md}>
            <PillsNav>
              {buttons}
            </PillsNav>
          </Col>
        </Row>
    
    return Nav

module.exports = FilterButtons
