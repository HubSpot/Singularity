Controller = require './Controller'
Racks = require '../collections/Racks'
Request = require '../models/Request'
RequestFormEditView = require 'views/requestFormEdit'

class RequestFormEditController extends Controller

    initialize: ({@requestId}) ->

        @models.request = new Request id: @requestId
        @models.request.raw = true
        @collections.racks    = new Racks []

        @setView new RequestFormEditView
            requestId: @requestId
            model: @models.request
            racks: @collections.racks

        $.when( @models.request.fetch(), @collections.racks.fetch() ).then =>
            @view.renderEditForm()

        app.showView @view


module.exports = RequestFormEditController