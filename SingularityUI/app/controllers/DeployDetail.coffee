Controller = require './Controller'

DeployDetails          = require '../models/DeployDetails'
RequestHistoricalTasks = require '../collections/RequestHistoricalTasks'

DeployDetailView       = require '../views/deploy'
ExpandableTableSubview = require '../views/expandableTableSubview'
SimpleSubview          = require '../views/simpleSubview'

class DeployDetailController extends Controller

  templates:
    header:             require '../templates/deployDetail/deployHeader'
    info:               require '../templates/deployDetail/deployInfo'
    tasks:              require '../templates/deployDetail/deployTasks'

  initialize: ({@requestId, @deployId}) ->
    #
    # Data stuff
    #
    @models.deploy = new DeployDetails
      deployId: @deployId
      requestId: @requestId

    @collections.deployTasks = new RequestHistoricalTasks([], {@requestId})

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
      model:      @models.deploy
      collection: @collections.deployTasks
      template:   @templates.tasks

    #
    # Main view & stuff
    #
    @setView new DeployDetailView _.extend {@requestId, @deployId, @subviews},
      model: @models.deploy

    @refresh()

    app.showView @view

  refresh: ->
    requestFetch = @models.deploy.fetch()
    if @collections.deployTasks.currentPage is 1
      @collections.deployTasks.fetch().error    @ignore404

module.exports = DeployDetailController
