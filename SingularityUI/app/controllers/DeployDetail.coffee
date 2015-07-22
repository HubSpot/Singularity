Controller = require './Controller'

DeployDetails          = require '../models/DeployDetails'
RequestHistoricalTasks = require '../collections/RequestHistoricalTasks'
RequestTasks           = require '../collections/RequestTasks'

DeployDetailView       = require '../views/deploy'
ExpandableTableSubview = require '../views/expandableTableSubview'
SimpleSubview          = require '../views/simpleSubview'

class DeployDetailController extends Controller

  templates:
    header:             require '../templates/deployDetail/deployHeader'
    info:               require '../templates/deployDetail/deployInfo'
    taskHistory:        require '../templates/deployDetail/deployTasks'
    activeTasks:        require '../templates/deployDetail/activeTasks'

  initialize: ({@requestId, @deployId}) ->
    #
    # Data stuff
    #
    @models.deploy = new DeployDetails
      deployId: @deployId
      requestId: @requestId

    @collections.taskHistory = new RequestHistoricalTasks [],
      requestId: @requestId

    @collections.activeTasks = new RequestTasks [],
        requestId: @requestId
        state:    'active'

    #
    # Subviews
    #
    @subviews.header = new SimpleSubview
      model:      @models.deploy
      template:   @templates.header

    @subviews.info = new SimpleSubview
      model:      @models.deploy
      template:   @templates.info

    @subviews.taskHistory = new SimpleSubview
      collection: @collections.taskHistory
      template:   @templates.taskHistory

    @collections.taskHistory.getTasksForDeploy(@deployId)
    @subviews.activeTasks = new SimpleSubview
      collection: @collections.activeTasks
      template:   @templates.activeTasks


    @collections.taskHistory.fetch().done =>
        @refresh()
        @setView new DeployDetailView _.extend {@requestId, @deployId, @subviews},
          model: @models.deploy

        app.showView @view

  refresh: ->
    requestFetch = @models.deploy.fetch()
    promise = @collections.taskHistory.fetch()
    promise.error =>
        @ignore404
    promise.done =>
        filtered = @collections.taskHistory.getTasksForDeploy(@deployId)
        # @collections.taskHistory.reset(filtered)

        @collections.taskHistory = new RequestHistoricalTasks filtered, @requestId
        console.log @collections.taskHistory.getTasksForDeploy(@deployId)

    promise = @collections.activeTasks.fetch()
    promise.error =>
        @ignore404

module.exports = DeployDetailController
