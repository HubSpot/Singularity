Controller = require './Controller'

NotFoundView = require '../views/notFound'

class NotFoundController extends Controller

    initialize: ->
        @setView new NotFoundView
        app.showView @view

module.exports = NotFoundController
