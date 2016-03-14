React = require 'react'

PlainText = React.createClass

    render: ->
        <div className={@props.prop.className}>
            {@props.prop.text}
        </div>

module.exports = PlainText
