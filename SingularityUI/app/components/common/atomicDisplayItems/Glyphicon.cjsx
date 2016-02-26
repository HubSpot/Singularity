React = require 'react'
classNames = require 'classnames'

Glyphicon = React.createClass

    render: ->
        className = classNames 'glyphicon', "glyphicon-#{@props.iconClass}"
        <span className=className aria-hidden='true' />

module.exports = Glyphicon
