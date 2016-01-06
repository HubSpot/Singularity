View = require './view'
Task = require '../models/Task'

killTemplate = require '../templates/vex/taskKill'
killOverrideTemplate = require '../templates/vex/taskKillOverride'
killDestroyTemplate = require '../templates/vex/taskKillDestroy'


class taskOverviewSubview extends View

    events: ->
        _.extend super,
            'click [data-action="remove"]': 'promptKillTask'

    initialize: ({@collection, @model, @template}) ->

        @taskModel = new Task id: @model.taskId

        for eventName in ['sync', 'add', 'remove', 'change']
            @listenTo @model, eventName, @render

        # copy latest task cleanup object over to the task model whenever things change
        for eventName in ['add', 'reset']
            @listenTo @collection, eventName, =>
                taskId = @model.get 'taskId'
                cleanup = _.last(@collection.filter (c) -> c.get('taskId').id is taskId)

                if cleanup
                    @model.set 'cleanup', cleanup.attributes
                else
                    @model.unset 'cleanup'

        @listenTo @model, 'reset', =>
            @$el.empty()


    render: ->
        return if not @model.synced
        @$el.html @template @renderData()

    renderData: ->
        config:         config
        data:           @model.toJSON()
        synced:         @model.synced and @collection.synced


    # Choose prompt based on if we plan to
    # gracefully kill (sigterm), or force kill (kill-9)
    promptKillTask: =>
        @model.fetch().done =>
            if @model.get 'isStillRunning'
                @collection.fetch({reset: true}).done =>
                    if @model.has('cleanup') and not @model.get('cleanup').isImmediate
                        btnText = 'Override'
                        templ = killOverrideTemplate
                    else if @model.get 'isCleaning'
                        btnText = 'Destroy task'
                        templ = killDestroyTemplate
                    else
                        btnText = 'Kill task'
                        templ = killTemplate

                    vex.dialog.confirm
                        buttons: [
                            $.extend {}, vex.dialog.buttons.YES,
                                text: btnText
                                className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                            vex.dialog.buttons.NO
                        ]
                        input: """
                            <input name="message" id="disable-healthchecks-message" type="text" placeholder="Message (optional)" />
                        """
                        message: templ id: @model.taskId

                        callback: (confirmed) =>
                            @killTask(confirmed) if confirmed


    killTask: (data) =>
        @taskModel.kill(data.message, @model.has('cleanup') or @model.get('isCleaning'))
            .done (data) =>
                @collection.add [data], parse: true  # automatically response  object to the cleanup collection
            .error (response) =>
                if response.status is 409  # HTTP 409 means a cleanup is already going on, nothing to flip out about
                    app.caughtError()
                    @collection.add [response.responseJSON], parse: true


module.exports = taskOverviewSubview
