Controller = require './Controller'

RequestDeployStatus                 = require '../models/RequestDeployStatus'
#Task                   = require '../collections/Task'

DeployDetailView      = require '../views/deploy'
ExpandableTableSubview = require '../views/expandableTableSubview'
SimpleSubview          = require '../views/simpleSubview'

class DeployDetailController extends Controller

  templates:
    header:             require '../templates/deployDetail/deployHeader'
    info:               require '../templates/deployDetail/deployInfo'
    #tasks:              require '../templates/deployDetail/deployTasks'

    initialize: ({@requestId, @deployId}) ->
      console.log('hi1')
      #
      # Data stuff
      #
      @models.deploy = new RequestDeployStatus
        deployId: @deployId
        requestId: @requestId

      #@collections.deployTasks = new Tasks [],
      #  requestId: @requestId

      #@collections.deployTasks = @collections.deployTasks.where({deployId: @deployId})

      #
      # Subviews
      #
      @subviews.header = new SimpleSubview
        model:      @models.deploy
        template:   @templates.header
      @subviews.info = new SimpleSubview
        model:      @models.deploy
        template:   @templates.info
      #@subviews.tasks = new SimpleSubview
      #  collection:      @collections.deployTasks
      #  template:   @templates.tasks


      #
      # Main view & stuff
      #
      @setView new DeployDetailView _.extend {@requestId, @deployId, @subviews},
        model: @models.deploy

      @refresh()

      app.showView @view

      refresh: ->
        requestFetch = @models.deploy.fetch()


module.exports = DeployDetailController
