Controller = require './Controller'

NewRequestView = require 'views/newRequest'

class NewRequestController extends Controller

    initialize: ->
        @setView new NewRequestView

        app.showView @view

module.exports = NewRequestController
