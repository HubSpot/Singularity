Controller = require './Controller'

RequestFormView = require 'views/requestForm'

class NewRequestController extends Controller

    initialize: ->
        @setView new RequestFormView
          type: 'create'

        app.showView @view

module.exports = NewRequestController
