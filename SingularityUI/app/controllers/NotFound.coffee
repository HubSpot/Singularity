Controller = require './Controller'

NotFoundView = require '../views/notFound'

class NotFoundController extends Controller

    initialize: ->
        app.showView new NotFoundView

module.exports = NotFoundController
