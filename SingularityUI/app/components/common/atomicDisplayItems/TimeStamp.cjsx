TimeStamp = React.createClass

    timeStampFromNow: ->
        return '' if not @props.prop.timestamp
        timeObject = moment @props.prop.timestamp
        return <div>{timeObject.fromNow()} ({ timeObject.format window.config.timestampFormat})</div>

    render: ->
        if @props.prop.display == 'timeStampFromNow'
            return @timeStampFromNow()

module.exports = TimeStamp
