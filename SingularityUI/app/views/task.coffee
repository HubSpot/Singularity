View = require './view'

class TaskView extends View

    baseTemplate: require '../templates/taskDetail/taskBase'

    events: ->
        _.extend super,
            'click [data-action="viewObjectJSON"]': 'viewJson'
            'click [data-action="viewJsonProperty"]': 'viewJsonProperty'
            'click [data-action="remove"]': 'killTask'
            'click #toggle-Healthchecks' : 'toggleHealthchecks'
    initialize: ({@taskId}) ->

    render: ->
        @$el.html @baseTemplate

        # Plop subview contents in there. It'll take care of everything itself
        @$('#overview').html        @subviews.overview.$el
        @$('#history').html         @subviews.history.$el
        @$('#file-browser').html    @subviews.fileBrowser.$el
        @$('#s3-logs').html         @subviews.s3Logs.$el
        @$('#lb-updates').html      @subviews.lbUpdates.$el
        @$('#health-checks').html   @subviews.healthChecks.$el
        @$('#info').html            @subviews.info.$el
        @$('#resources').html       @subviews.resourceUsage.$el
        @$('#environment').html     @subviews.environment.$el

    viewJson: (event) ->
        utils.viewJSON @model

    viewJsonProperty: (event) =>
        index = $(event.target).data('index')
        objKey = $(event.target).data('key')

        # Clone the model so we can use the viewJSON
        # method which requires a model
        modelClone = $.extend true, {}, @model
        modelClone.synced = true

        # remove unwanted attributes and
        # only keep the chosen attribute
        for own key, value of modelClone.attributes
            if key isnt objKey
                modelClone.unset key, {silent:true}
            else
                modelClone.attributes[key].splice 0, modelClone.attributes[key].length, value[index]

        utils.viewJSON modelClone

    killTask: (event) ->
        taskModel = new Task id: @taskId
        taskModel.promptKill =>
            setTimeout (=> @trigger 'refreshrequest'), 1000

    toggleHealthchecks: ->
        @model.set 'showRecentHealthchecksOnly', !@model.get 'showRecentHealthchecksOnly'


module.exports = TaskView