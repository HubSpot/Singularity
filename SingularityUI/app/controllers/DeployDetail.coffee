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

    @subviews.taskHistory = new ExpandableTableSubview
      collection: @collections.taskHistory
      template:   @templates.taskHistory

    @subviews.activeTasks = new ExpandableTableSubview
      collection: @collections.activeTasks
      template:   @templates.activeTasks

    #
    # Main view & stuff
    #
    @setView new DeployDetailView _.extend {@requestId, @deployId, @subviews},
      model: @models.deploy

    @refresh()

    app.showView @view

  refresh: ->
    requestFetch = @models.deploy.fetch()
    if @collections.taskHistory.currentPage is 1
      @collections.taskHistory.fetch().error    @ignore404
    if @collections.activeTasks.currentPage is 1
      @collections.activeTasks.fetch().error    @ignore404

module.exports = DeployDetailController
