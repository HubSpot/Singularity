React = require 'react'
Glyphicon = require '../common/atomicDisplayItems/Glyphicon'

Header = React.createClass

    render: ->
        <div>
            {<a 
                className='btn btn-danger' 
                href={window.config.appRoot + '/request/' + @props.requestId} 
                alt={'Return to Request ' + @props.requestId}>
                <Glyphicon iconClass='arrow-left' /> Back to {@props.requestId}
            </a> unless @props.global}
            <h1>{'Global' if @props.global} Historical Tasks</h1>
        </div>

module.exports = Header
