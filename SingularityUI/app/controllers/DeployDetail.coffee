Controller = require './Controller'

DeployDetails           = require '../models/DeployDetails'
HistoricalTasks   = require '../collections/HistoricalTasks'
DeployActiveTasks   = require '../collections/DeployActiveTasks'
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
    @title "#{ @requestId } deploy #{ @deployId }"

    #
    # Data stuff
    #
    @models.deploy = new DeployDetails
      deployId: @deployId
      requestId: @requestId

    @collections.taskHistory = new HistoricalTasks [],
      params:
        requestId: @requestId
        deployId: @deployId

    @collections.activeTasks = new DeployActiveTasks [],
      requestId: @requestId
      deployId: @deployId

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

    @subviews.activeTasks = new SimpleSubview
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

    promise = @collections.taskHistory.fetch()
    promise.error =>
      @ignore404

    promise = @collections.activeTasks.fetch()
    promise.error =>
      @ignore404
    promise.done =>
      # Get the latest health check for each active task
      @collections.healthChecks.reset()
      for task in @collections.activeTasks.models
          health = new HealthCheckResult
              taskId: task.id
          health.fetch(success: =>
              if health.get('durationMillis')
                  @collections.healthChecks.add(health)
          )

module.exports = DeployDetailController
