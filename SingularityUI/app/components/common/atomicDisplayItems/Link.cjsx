React = require 'react'
OverlayTrigger = require 'react-bootstrap/lib/OverlayTrigger'
ToolTip = require 'react-bootstrap/lib/Tooltip'

Link = React.createClass

    getLink: ->
        <a href={@props.prop.url} title={@props.prop.title} onClick={@props.prop.onClickFn} className={@props.prop.className} id={@props.id} key={@props.key}>
            {@props.prop.text}
        </a>

    getToolTip: ->
        <ToolTip id={@props.prop.overlayId}>{@props.prop.overlayToolTipContent}</ToolTip>

    render: ->
        if @props.prop.overlayTrigger
            <OverlayTrigger placement={@props.prop.overlayTriggerPlacement} overlay={@getToolTip()}>
                {@getLink()}
            </OverlayTrigger>
        else
            @getLink()

module.exports = Link
