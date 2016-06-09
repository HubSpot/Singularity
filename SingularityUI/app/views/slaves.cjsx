View = require './view'

Slave = require '../models/Slave'
Slaves = require '../collections/Slaves'
SlavesPage = require '../components/machines/Slaves'

React = require 'react'
ReactDOM = require 'react-dom'

class SlavesView extends View

    initialPageLoad: true

    initialize: ({@state}) ->

    render: ->
        return if not @collection.synced and @collection.isEmpty?()

        ReactDOM.render(
            <SlavesPage
                slaves = @collection
            />,
            @el)        

        if @state and @initialPageLoad
            return if @state is 'all'
            utils.scrollTo "##{@state}"
            @initialPageLoad = false

module.exports = SlavesView
