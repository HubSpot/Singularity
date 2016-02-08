TimeStamp = React.createClass

    timeStampFromNow: ->
        return '' if not @props.timestamp
        timeObject = moment @props.timestamp
        return <div>{timeObject.fromNow()} ({ timeObject.format window.config.timestampFormat})</div>

    render: ->
        if @props.display == 'timeStampFromNow'
            return @timeStampFromNow()

module.exports = TimeStamp