Controller = require './Controller'
Racks = require '../collections/Racks'
RequestFormNewView = require 'views/requestFormNew'

class RequestFormNewController extends Controller

    initialize: ->

      @collections.racks    = new Racks []

      @setView new RequestFormNewView
        racks: @collections.racks

      @collections.racks.fetch()

      app.showView @view


module.exports = RequestFormNewController