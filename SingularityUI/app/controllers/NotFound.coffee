Controller = require './Controller'

NotFoundView = require '../views/notFound'

class NotFoundController extends Controller

    initialize: ->
        @title 'Not Found'
        @setView new NotFoundView
        app.showView @view

module.exports = NotFoundController
