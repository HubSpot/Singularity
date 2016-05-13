View = require './view'

Deploy = require '../models/Deploy'
TaskFiles = require '../collections/TaskFiles'
TaskHistory = require '../models/TaskHistory'

class RequestView extends View

    template: require '../templates/requestDetail/requestBase'

    events: ->
        _.extend super,
            'click [data-action="viewJSON"]': 'viewJson'
            'click [data-action="viewObjectJSON"]': 'viewObjectJson'

            'click [data-action="remove"]': 'removeRequest'
            'click [data-action="run-request-now"]': 'runRequest'
            'click [data-action="rerun-task"]': 'rerunTask'
            'click [data-action="pause"]': 'pauseRequest'
            'click [data-action="scale"]': 'scaleRequest'
            'click [data-action="unpause"]': 'unpauseRequest'
            'click [data-action="bounce"]': 'bounceRequest'
            'click [data-action="exit-cooldown"]': 'exitCooldownRequest'
            'click [data-action="starToggle"]': 'toggleStar'

            'click [data-action="disableHealthchecks"]': 'disableHealthchecks'
            'click [data-action="enableHealthchecks"]': 'enableHealthchecks'

            'click [data-action="run-now"]': 'runTask'

            'click [data-action="expand-deploy-history"]': 'flashDeployHistory'

            'click [data-action="makeScalePermanent"]': 'makeScalePermanent'
            'click [data-action="makePausePermanent"]': 'makePausePermanent'
            'click [data-action="makeSkipHealthchecksPermanent"]': 'makeSkipHealthchecksPermanent'
            'click [data-action="cancelBounce"]': 'cancelBounce'

            'click [data-action="revertPause"]': 'revertPause'
            'click [data-action="revertScale"]': 'revertScale'
            'click [data-action="revertSkipHealthchecks"]': 'revertSkipHealthchecks'

            'click [data-action="stepDeploy"]': 'stepDeploy'
            'click [data-action="cancelDeploy"]': 'cancelDeploy'

    initialize: ({@requestId}) ->

    render: ->
        @$el.html @template
            config: config

        # Attach subview elements
        @$('#header').html              @subviews.header.$el
        @$('#request-history-msg').html @subviews.requestHistoryMsg.$el
        @$('#stats').html               @subviews.stats.$el
        @$('#active-tasks').html        @subviews.activeTasks.$el
        @$('#scheduled-tasks').html     @subviews.scheduledTasks.$el
        @$('#task-history').html        @subviews.taskHistory.$el
        @$('#deploy-history').html      @subviews.deployHistory.$el
        @$('#request-history').html     @subviews.requestHistory.$el
        @$('#request-action-expirations').html @subviews.actionExpirations.$el

        super.afterRender()

    viewJson: (e) =>
        $target = $(e.currentTarget).parents 'tr'
        id = $target.data 'id'
        collectionName = $target.data 'collection'

        # Need to reach into subviews to get the necessary data
        collection = @subviews[collectionName].collection
        utils.viewJSON collection.get id

    viewObjectJson: (e) =>
        utils.viewJSON @model

    removeRequest: (e) =>
        @model.promptRemove =>
            app.router.navigate 'requests', trigger: true

    runRequest: (e, taskId) => # If taskId is provided, rerun the task. Else run the task.
        callback = (data) =>
            unless data.afterStart in ['browse-to-sandbox', 'autoTail']
                @trigger 'refreshrequest'
                setTimeout ( => @trigger 'refreshrequest'), 2500
        if taskId
            task = new TaskHistory {taskId}
            task.fetch()
                .done =>
                    @model.promptRun callback, task
        else 
            @model.promptRun callback

    rerunTask: (e) =>
        taskId = e.target.getAttribute 'data-taskId'
        @runRequest e, taskId

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

    disableHealthchecks: (e) =>
        @model.promptDisableHealthchecks =>
            @trigger 'refreshrequest'

    enableHealthchecks: (e) =>
        @model.promptEnableHealthchecks =>
            @trigger 'refreshrequest'

    exitCooldownRequest: (e) =>
        @model.promptExitCooldown =>
            @trigger 'refreshrequest'

    makeScalePermanent: (e) =>
        @model.makeScalePermanent =>
            @trigger 'refreshrequest'

    makePausePermanent: (e) =>
        @model.makePausePermanent =>
            @trigger 'refreshrequest'

    makeSkipHealthchecksPermanent: (e) =>
        @model.makeSkipHealthchecksPermanent =>
            @trigger 'refreshrequest'

    cancelBounce: (e) =>
        @model.cancelBounce =>
            @trigger 'refreshrequest'

    revertPause: (e) =>
        @model.promptUnpause =>
            @makePausePermanent()

    revertScale: (e) =>
        @model.scale
            instances: $(e.target).attr('data-revert-param')
        @makeScalePermanent()

    revertSkipHealthchecks: (e) =>
        skipHealthchecks = $(e.target).attr('data-revert-param')
        if skipHealthchecks == 'true'
            @model.disableHealthchecks()
        else
            @model.enableHealthchecks()
        @makeSkipHealthchecksPermanent()

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

    toggleStar: (e) ->
        $target = $(e.currentTarget)
        id = $target.attr('data-id')
        @collection.toggleStar id

        starred = $target.attr('data-starred') is "true"
        if starred
            $target.attr 'data-starred', 'false'
        else
            $target.attr 'data-starred', 'true'

    stepDeploy: (e) =>
        @model.promptStepDeploy =>
            @trigger 'refreshrequest'

    cancelDeploy: (e) =>
        @model.promptCancelDeploy =>
            @trigger 'refreshrequest'

module.exports = RequestView
