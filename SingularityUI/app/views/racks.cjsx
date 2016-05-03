View = require './view'

Rack = require '../models/Rack'
Racks = require '../collections/Racks'
RacksPage = require '../components/machines/Racks'

React = require 'react'
ReactDOM = require 'react-dom'

class RacksView extends View

    template: require '../templates/racks/base'
    rackTemplate: require '../templates/racks/rack'
    
    initialPageLoad: true

    initialize: ({@state}) ->
        for eventName in ['sync', 'add', 'remove', 'change']
            @listenTo @collection, eventName, @render

        @listenTo @collection, 'reset', =>
            @$el.empty()

    render: ->
        return if not @collection.synced and @collection.isEmpty?()
        @$el.html @template()

        ReactDOM.render(
            <RacksPage
                racks = @collection
                refresh = {() => @trigger 'refreshrequest'}
            />,
            @el)

        if @state and @initialPageLoad
            return if @state is 'all'
            utils.scrollTo "##{@state}"
            @initialPageLoad = false

        super.afterRender()


module.exports = RacksView
