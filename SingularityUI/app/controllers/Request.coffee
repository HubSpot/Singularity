Controller = require './ReactController'
RequestView = require '../views/RequestView'

class RequestController extends Controller

  initialize: ({@requestId}) ->
    app.showPageLoader()

    new RequestView

module.exports = RequestController