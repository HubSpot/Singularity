Controller = require './Controller'

Requests = require '../collections/Requests'

RequestsTableView = require '../views/requests'

class RequestsTableController extends Controller

    initialize: ({@state, @subFilter, @searchFilter}) ->
        @title 'Requests'

        # We want the view to handle the page loader for this one
        @collections.requests = new Requests [], {@state}

        @setView new RequestsTableView _.extend {@state, @subFilter, @searchFilter},
            collection: @collections.requests

        @collections.requests.fetch()

        app.showView @view

    refresh: ->
        # Don't refresh if user is scrolled down, viewing the table (arbitrary value)
        return if $(window).scrollTop() > 200
        # Don't refresh if the table is sorted
        return if @view.isSorted

        @collections.requests.fetch reset: true

module.exports = RequestsTableController
