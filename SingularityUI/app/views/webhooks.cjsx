React = require 'react'
ReactDOM = require 'react-dom'

Webhooks = require '../components/webhooks/Webhooks'

View = require './view'

class WebhooksView extends View

    initialize: ({@collections, @fetchedWebhooks}, opts) ->

    render: ->
        $(@el).addClass("webhooks-root")
        ReactDOM.render(
            <Webhooks
                fetchedWebhooks = {@fetchedWebhooks}
                collections = {@collections}
            />,
            @el);


module.exports = WebhooksView
