IconButton = React.createClass

    render: ->
        <button 
            aria-label = @props.ariaLabel
            alt = @props.ariaLabel
            className = {classNames @props.className, 'glyphicon'}
            onClick = {@props.onClick} 
        />

module.exports = IconButton
