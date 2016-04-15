React = require 'react'
classNames = require 'classnames'

IconButton = React.createClass

    propTypes:
        prop: React.PropTypes.shape({
            ariaLabel: React.PropTypes.string.isRequired
            className: React.PropTypes.oneOfType([
                React.PropTypes.string,
                React.PropTypes.object #Classnames object
            ])
            btn: React.PropTypes.boolean
            btnClass: React.PropTypes.string.isRequired
            iconClass: React.PropTypes.string.isRequired
            onClick: React.PropTypes.func.isRequired
        }).isRequired

    render: ->
        <button 
            aria-label = @props.prop.ariaLabel
            alt = @props.prop.ariaLabel
            className = {classNames @props.prop.className, {'btn': @props.prop.btn isnt false}, "btn-#{@props.prop.btnClass}", 'glyphicon', "glyphicon-#{@props.prop.iconClass}"}
            onClick = {@props.prop.onClick} 
        />

module.exports = IconButton
