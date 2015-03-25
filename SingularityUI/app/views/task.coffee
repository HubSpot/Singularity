View = require './view'

Task = require '../models/Task'

class TaskView extends View

    baseTemplate: require '../templates/taskDetail/taskBase'

    events: ->
        _.extend super,
            'click [data-action="viewObjectJSON"]': 'viewJson'
            'click [data-action="viewLbJSON"]': 'viewLoadBalancerJSON'
            'click [data-action="remove"]': 'killTask'

    initialize: ({@taskId}) ->

    render: ->
        @$el.html @baseTemplate

        # Plop subview contents in there. It'll take care of everything itself
        @$('#overview').html     @subviews.overview.$el
        @$('#history').html      @subviews.history.$el
        @$('#file-browser').html @subviews.fileBrowser.$el
        @$('#s3-logs').html      @subviews.s3Logs.$el
        @$('#lb-updates').html   @subviews.lbUpdates.$el
        @$('#info').html         @subviews.info.$el
        @$('#resources').html    @subviews.resourceUsage.$el
        @$('#environment').html  @subviews.environment.$el

    viewJson: (event) ->
        utils.viewJSON @model

    viewLoadBalancerJSON: (event) =>
        lbIndex = $(event.target).data('lbIndex')

        # Clone the model so we can use the viewJSON
        # method which requires a model
        lbModel = @model.clone()
        lbModel.synced = true

        # remove unwanted attributes and
        # only keep the chosen LB info
        for own key, value of lbModel.attributes
            if key isnt 'loadBalancerUpdates'
                lbModel.unset key, {silent:true}
            else
                lbModel.attributes[key].splice 0, lbModel.attributes[key].length, value[lbIndex]

        utils.viewJSON lbModel

    killTask: (event) ->
        taskModel = new Task id: @taskId
        taskModel.promptKill =>
            setTimeout (=> @trigger 'refreshrequest'), 1000

module.exports = TaskView
