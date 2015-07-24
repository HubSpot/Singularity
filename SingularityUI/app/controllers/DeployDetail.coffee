Controller = require './Controller'

DeployDetails           = require '../models/DeployDetails'
RequestHistoricalTasks  = require '../collections/RequestHistoricalTasks'
RequestTasks            = require '../collections/RequestTasks'
HealthCheckResult       = require '../models/HealthCheckResult'
DeployTasksHealthChecks = require '../collections/DeployTasksHealthChecks'

DeployDetailView       = require '../views/deploy'
ExpandableTableSubview = require '../views/expandableTableSubview'
SimpleSubview          = require '../views/simpleSubview'

class DeployDetailController extends Controller

  templates:
    header:             require '../templates/deployDetail/deployHeader'
    info:               require '../templates/deployDetail/deployInfo'
    taskHistory:        require '../templates/deployDetail/deployTasks'
    activeTasks:        require '../templates/deployDetail/activeTasks'
    healthChecks:       require '../templates/deployDetail/deployHealthChecks'

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

    @collections.healthChecks = new DeployTasksHealthChecks []

    #
    # Subviews
    #
    @subviews.header = new SimpleSubview
      model:      @models.deploy
      template:   @templates.header

    @subviews.info = new SimpleSubview
      model:      @models.deploy
      template:   @templates.info

    @subviews.taskHistory = new ExpandableTableSubview
      collection: @collections.taskHistory
      template:   @templates.taskHistory

    @subviews.activeTasks = new ExpandableTableSubview
      collection: @collections.activeTasks
      template:   @templates.activeTasks

    @subviews.healthChecks = new SimpleSubview
        collection: @collections.healthChecks
        template:   @templates.healthChecks

    @refresh()
    @setView new DeployDetailView _.extend {@requestId, @deployId, @subviews},
      model: @models.deploy

    app.showView @view

  refresh: ->
    requestFetch = @models.deploy.fetch()

    @collections.taskHistory.atATime = 999999
    promise = @collections.taskHistory.fetch()
    promise.error =>
        @ignore404
    promise.done =>
        filtered = @collections.taskHistory.getTasksForDeploy(@deployId)
        @collections.taskHistory.atATime = 5
        @collections.taskHistory.reset(filtered)

    @collections.taskHistory.atATime = 999999
    promise = @collections.activeTasks.fetch()
    promise.error =>
        @ignore404
    promise.done =>
        filtered = @collections.activeTasks.getTasksForDeploy(@deployId)
        @collections.taskHistory.atATime = 5
        @collections.activeTasks.reset(filtered)

        # Get the latest health check for each active task
        @collections.healthChecks.reset()
        for task in filtered
            health = new HealthCheckResult
                taskId: task.id
            health.fetch()
            @collections.healthChecks.add(health)

module.exports = DeployDetailController
