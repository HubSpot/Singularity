React = require 'react'
ReactDOM = require 'react-dom'

Webhooks = require '../components/webhooks/Webhooks'

View = require './view'

class WebhooksView extends View

    initialize: ({@collections, @fetched}, opts) ->

    render: ->
        $(@el).addClass("webhooks-root")
        ReactDOM.render(
            <Webhooks
                fetched = @fetched
                collections = {@collections}
            />,
            @el);


module.exports = WebhooksView
