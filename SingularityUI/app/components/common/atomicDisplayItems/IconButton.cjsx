IconButton = React.createClass

    render: ->
        <button 
            aria-label = @props.ariaLabel
            alt = @props.ariaLabel
            className = {classNames @props.className, {'btn': @props.btn isnt false}, "btn-#{@props.btnClass}", 'glyphicon', "glyphicon-#{@props.iconClass}"}
            onClick = {@props.onClick} 
        />

module.exports = IconButton
