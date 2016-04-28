React = require 'react'

PlainText = React.createClass

    propTypes:
        prop: React.PropTypes.shape({
            text: React.PropTypes.node.isRequired
            className: React.PropTypes.string
        }).isRequired

    render: ->
        <div className={@props.prop.className}>
            {@props.prop.text}
        </div>

module.exports = PlainText
