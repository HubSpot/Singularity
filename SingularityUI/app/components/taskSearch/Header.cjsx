Header = React.createClass

    render: ->
        <div>
            {<a 
                className='btn btn-danger' 
                href={window.config.appRoot + '/request/' + @props.requestId} 
                alt={'Return to Request ' + @props.requestId}>
                <span className='glyphicon glyphicon-arrow-left' aria-hidden='true' /> Back to {@props.requestId}
            </a> unless @props.global}
            <h1>{'Global' if @props.global} Task Search</h1>
        </div>

module.exports = Header