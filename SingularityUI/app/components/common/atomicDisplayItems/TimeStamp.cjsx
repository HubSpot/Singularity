React = require 'react'
moment = require 'moment'

TimeStamp = React.createClass

    propTypes:
        prop: React.PropTypes.shape({
            timestamp: React.PropTypes.number.isRequired
            className: React.PropTypes.string
            display: React.PropTypes.string.isRequired
        }).isRequired

    timeStampFromNow: ->
        timeObject = moment @props.prop.timestamp
        return <div className={@props.prop.className}>{timeObject.fromNow()} ({ timeObject.format window.config.timestampFormat})</div>

    absoluteTimestamp: ->
        timeObject = moment @props.prop.timestamp
        return <div className={@props.prop.className}>{ timeObject.format window.config.timestampFormat }</div>

    duration: ->
        return <div className={@props.prop.className}>{ moment.duration(@props.prop.timestamp).humanize() }</div>

    render: ->
        if @props.prop.display is 'timeStampFromNow'
            return @timeStampFromNow()
        else if @props.prop.display is 'absoluteTimestamp'
            return @absoluteTimestamp()
        else if @props.prop.display is 'duration'
            return @duration()

module.exports = TimeStamp
