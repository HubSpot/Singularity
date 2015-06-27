Controller = require './ReactController'
RequestView = require '../views/RequestView'

Request                = require '../models/Request'
RequestDeployStatus    = require '../models/RequestDeployStatus'

Tasks                  = require '../collections/Tasks'
RequestTasks           = require '../collections/RequestTasks'
RequestHistoricalTasks = require '../collections/RequestHistoricalTasks'
RequestDeployHistory   = require '../collections/RequestDeployHistory'
RequestHistory         = require '../collections/RequestHistory'

class RequestController extends Controller

  initialize: ({@requestId}) ->
    app.showPageLoader()

    @models.request = new Request id: @requestId

    @models.activeDeployStats = new RequestDeployStatus
      requestId: @requestId
      deployId:  undefined

    @collections.activeTasks = new RequestTasks [],
      requestId: @requestId
      state:    'active'

    @collections.scheduledTasks = new Tasks [],
      requestId: @requestId
      state:     'scheduled'

    @collections.requestHistory  = new RequestHistory         [], {@requestId}
    @collections.taskHistory     = new RequestHistoricalTasks [], {@requestId}
    @collections.deployHistory   = new RequestDeployHistory   [], {@requestId}

    new RequestView
      collections: @collections
      models: @models


module.exports = RequestController