View = require './view'

Rack = require '../models/Rack'
Racks = require '../collections/Racks'
RacksPage = require '../components/machines/Racks'

React = require 'react'
ReactDOM = require 'react-dom'

class RacksView extends View
    
    initialPageLoad: true

    initialize: ({@state}) ->

    render: ->
        return if not @collection.synced and @collection.isEmpty?()

        ReactDOM.render(
            <RacksPage
                racks = @collection
            />,
            @el)

        if @state and @initialPageLoad
            return if @state is 'all'
            utils.scrollTo "##{@state}"
            @initialPageLoad = false


module.exports = RacksView
