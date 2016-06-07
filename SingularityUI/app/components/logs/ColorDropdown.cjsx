React = require 'react'
classNames = require 'classnames'

{ connect } = require 'react-redux'
selectLogColor = require('../../actions/log').selectLogColor

class ColorDropdown extends React.Component
  renderColorChoices: ->
    activeColor = @props.activeColor

    @props.colors.map (color, index) =>
      colorClass = color.toLowerCase().replace(' ', '-')
      className = classNames
        active: @props.activeColor is colorClass
      <li key={index} className={className}>
        <a onClick={=> @props.selectLogColor(colorClass)}>
          {color}
        </a>
      </li>

  render: ->
    <div className="btn-group" title="Select Color Scheme">
      <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        <span className="glyphicon glyphicon-adjust"></span> <span className="caret"></span>
      </button>
      <ul className="dropdown-menu dropdown-menu-right">
        {@renderColorChoices()}
      </ul>
    </div>

mapStateToProps = (state) ->
  colors: state.colors
  activeColor: state.activeColor

mapDispatchToProps = { selectLogColor }

module.exports = connect(mapStateToProps, mapDispatchToProps)(ColorDropdown)
