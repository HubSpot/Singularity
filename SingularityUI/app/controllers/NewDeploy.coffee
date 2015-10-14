Controller = require './Controller'

NewDeployView = require 'views/newDeploy'

Request = require 'models/Request'

class NewDeployController extends Controller

    initialize: ({requestId}) ->
        app.showPageLoader()
        @title 'New Deploy'

        @models.request = new Request id: requestId

        @models.request.fetch().done =>
            @setView new NewDeployView
                model: @models.request

            app.showView @view

module.exports = NewDeployController
