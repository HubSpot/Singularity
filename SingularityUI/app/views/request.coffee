View = require './view'

Deploy = require '../models/Deploy'

class RequestView extends View

    template: require '../templates/requestDetail/requestBase'

    events: ->
        _.extend super,
            'click [data-action="viewJSON"]': 'viewJson'
            'click [data-action="viewObjectJSON"]': 'viewObjectJson'

            'click [data-action="remove"]': 'removeRequest'
            'click [data-action="run-request-now"]': 'runRequest'
            'click [data-action="pause"]': 'pauseRequest'
            'click [data-action="scale"]': 'scaleRequest'
            'click [data-action="unpause"]': 'unpauseRequest'
            'click [data-action="bounce"]': 'bounceRequest'

            'click [data-action="run-now"]': 'runTask'

            'click [data-action="expand-deploy-history"]': 'flashDeployHistory'

    initialize: ({@requestId}) ->

    render: ->
        @$el.html @template
          config: config

        # Attach subview elements
        @$('#header').html           @subviews.header.$el
        @$('#stats').html            @subviews.stats.$el
        @$('#active-tasks').html     @subviews.activeTasks.$el
        @$('#scheduled-tasks').html  @subviews.scheduledTasks.$el
        @$('#task-history').html     @subviews.taskHistory.$el
        @$('#deploy-history').html   @subviews.deployHistory.$el
        @$('#request-history').html  @subviews.requestHistory.$el

    viewJson: (e) =>
        $target = $(e.currentTarget).parents 'tr'
        id = $target.data 'id'
        collectionName = $target.data 'collection'

        if collectionName is 'deployHistory'
            deploy = new Deploy {},
                requestId: @model.id
                deployId:  id

            utils.viewJSON deploy
        else
            # Need to reach into subviews to get the necessary data
            collection = @subviews[collectionName].collection
            utils.viewJSON collection.get id

    viewObjectJson: (e) =>
        utils.viewJSON @model

    removeRequest: (e) =>
        @model.promptRemove =>
            app.router.navigate 'requests', trigger: true

    runRequest: (e) =>
        @model.promptRun =>
            @trigger 'refreshrequest'
            setTimeout =>
                @trigger 'refreshrequest'
            , 2500

    scaleRequest: (e) =>
        @model.promptScale =>
            @trigger 'refreshrequest'
  
    pauseRequest: (e) =>
        @model.promptPause =>
            @trigger 'refreshrequest'

    unpauseRequest: (e) =>
        @model.promptUnpause =>
            @trigger 'refreshrequest'
    
    bounceRequest: (e) =>
        @model.promptBounce =>
            @trigger 'refreshrequest'

    runTask: (e) =>
        id = $(e.target).parents('tr').data 'id'

        @model.promptRun =>
            @subviews.scheduledTasks.collection.remove id
            @subviews.scheduledTasks.render()

            setTimeout =>
                @trigger 'refreshrequest'
            , 3000

    flashDeployHistory: ->
        @subviews.deployHistory.flash()

module.exports = RequestView
