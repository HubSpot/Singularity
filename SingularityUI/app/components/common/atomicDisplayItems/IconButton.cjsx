React = require 'react'
classNames = require 'classnames'

IconButton = React.createClass

    render: ->
        <button 
            aria-label = @props.prop.ariaLabel
            alt = @props.prop.ariaLabel
            className = {classNames @props.prop.className, {'btn': @props.prop.btn isnt false}, "btn-#{@props.prop.btnClass}", 'glyphicon', "glyphicon-#{@props.prop.iconClass}"}
            onClick = {@props.prop.onClick} 
        />

module.exports = IconButton
