React = require 'react'

AdminOnly = React.createClass

    propTypes:
        children: React.PropTypes.node.isRequired
        showPermissionDeniedIfNotAdmin: React.PropTypes.bool.isRequired

    render: ->
        if app.hasAdminRights()
            return @props.children
        else if @props.showPermissionDeniedIfNotAdmin
            return <div>
                <h1> Permission Denied </h1>
                <div className='alert alert-danger'> You must have admin rights to view this page </div>
            </div>
        else
            return false

module.exports = AdminOnly
