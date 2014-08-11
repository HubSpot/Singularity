Controller = require './Controller'

NewDeployView = require 'views/newDeploy'

class NewDeployController extends Controller

    initialize: ({requestId}) ->
        @setView new NewDeployView {requestId}
        app.showView @view

module.exports = NewDeployController
