Glyphicon = React.createClass

    render: ->
        className = classNames 'glyphicon', @props.iconClass
        <span className=className aria-hidden='true' />

module.exports = Glyphicon
