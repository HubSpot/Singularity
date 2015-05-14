PillsNav = React.createClass
  
  displayName: 'PillsNav'

  render: ->
    <nav>
      <ul className='nav nav-pills'>
        {this.props.children}      
      </ul>
    </nav>

module.exports = PillsNav