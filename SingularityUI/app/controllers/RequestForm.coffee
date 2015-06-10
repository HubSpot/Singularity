Controller = require './Controller'
Racks = require '../collections/Racks'
Request = require '../models/Request'
RequestFormView = require 'views/requestForm'

class RequestFormController extends Controller

    initialize: ({@requestId, @type}) ->

      @models.request = new Request id: @requestId
      @collections.racks    = new Racks []

      @setView new RequestFormView
        type: @type
        requestId: @requestId
        model: @models.request
        racks: @collections.racks

      if @type is 'edit'
        $.when( @models.request.fetch(), @collections.racks.fetch() ).then =>
          @view.renderEditForm()

      else
        @collections.racks.fetch()

      app.showView @view


module.exports = RequestFormController