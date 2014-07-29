View = require './view'

class RequestView extends View

    template: require '../templates/requestDetail/requestBase'

    events: ->
        _.extend super,
            'click [data-action="viewJSON"]': 'viewJson'
            'click [data-action="viewObjectJSON"]': 'viewObjectJson'
            'click [data-action="viewRequestHistoryJSON"]': 'viewRequestHistoryJson'

            'click [data-action="remove"]': 'removeRequest'
            'click [data-action="run-request-now"]': 'runRequest'
            'click [data-action="pause"]': 'pauseRequest'
            'click [data-action="unpause"]': 'unpauseRequest'
            'click [data-action="bounce"]': 'bounceRequest'

            'click [data-action="run-now"]': 'runTask'

            'click [data-action="expand-deploy-history"]': 'expandDeployHistory'

    initialize: ({@requestId}) ->

    render: ->
        @$el.html @template()

        # Attach subview elements
        @$('#header').html           @subviews.header.$el
        @$('#stats').html            @subviews.stats.$el
        @$('#active-tasks').html     @subviews.activeTasks.$el
        @$('#task-history').html     @subviews.taskHistory.$el
        @$('#deploy-history').html   @subviews.deployHistory.$el
        @$('#request-history').html  @subviews.requestHistory.$el

    viewJson: (e) =>
        utils.viewJSON 'task', $(e.target).data('task-id')

    viewObjectJson: (e) =>
        utils.viewJSON 'request', $(e.target).data('request-id')

    viewRequestHistoryJson: (e) =>
        utils.viewJSON 'requestHistory', $(e.target).data('local-request-history-id')

    removeRequest: (e) =>
        @model.promptRemove =>
            app.router.navigate 'requests', trigger: true

    runRequest: (e) =>
        @model.promptRun =>
            @refresh()

    pauseRequest: (e) =>
        @model.promptPause =>
            @refresh()

    unpauseRequest: (e) =>
        @model.promptUnpause =>
            @refresh()
    
    bounceRequest: (e) =>
        @model.promptBounce =>
            @refresh()

    runTask: (e) =>
        $row = $(e.target).parents('tr')
        $containingTable = $row.parents('table')
        taskModel = app.collections.tasksScheduled.get($(e.target).data('task-id'))
        
        @model.promptRun =>
            app.collections.tasksScheduled.remove(taskModel)
            $row.remove()
            utils.handlePotentiallyEmptyFilteredTable $containingTable, 'task'

    expandDeployHistory: ->
        @subviews.deployHistory.expand()

module.exports = RequestView
