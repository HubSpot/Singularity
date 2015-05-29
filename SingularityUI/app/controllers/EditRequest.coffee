Controller = require './Controller'

Requests = require '../collections/Requests'

RequestFormView = require 'views/requestForm'

class NewRequestController extends Controller

    initialize: ({@requestId}) ->
      @collections.requests = new Requests [], {@state}

      @setView new RequestFormView
        type: 'edit'
        requestId: @requestId
        collection: @collections.requests

      @collections.requests.fetch()

      app.showView @view

module.exports = NewRequestController
