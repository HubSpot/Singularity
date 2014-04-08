View = require './view'

RequestHistoricalTasksTableView = require './requestHistoricalTasksTable'

Request = require '../models/Request'
RequestHistory = require '../models/RequestHistory'
RequestActiveDeploy = require '../models/RequestActiveDeploy'
RequestDeployHistory = require '../models/RequestDeployHistory'

RequestTasks = require '../collections/RequestTasks'
HistoricalTasks = require '../collections/HistoricalTasks'

class RequestView extends View

    template: require './templates/request'

    requestHeaderTemplate: require './templates/requestHeader'
    requestTasksActiveTableTemplate: require './templates/requestTasksActiveTable'
    requestTasksScheduledTableTemplate: require './templates/requestTasksScheduledTable'
    requestDeployHistoryTemplate: require './templates/requestDeployHistory'
    requestHistoryTemplate: require './templates/requestHistory'
    requestActiveDeployTemplate: require './templates/requestActiveDeploy'
    requestInfoTemplate: require './templates/requestInfo'

    removeRequestTemplate: require './templates/vex/removeRequest'

    initialize: ->
        @requestModel = new Request id: @options.requestId
        @requestHistory = new RequestHistory {}, requestId: @options.requestId
        @requestDeployHistory = new RequestDeployHistory {}, requestId: @options.requestId
        @requestTasksActive = new RequestTasks [], { requestId: @options.requestId, active: true }
        @requestActiveDeploy = { attributes: {} }

    fetch: ->
        promises = []

        @requestModel.fetched = false
        @requestHistory.fetched = false
        @requestDeployHistory.fetched = false
        @requestTasksActive.fetched = false

        promises.push @requestModel.fetch().done =>
            @requestModel.fetched = true
            @render()

            if @requestModel.get('activeDeploy')?
                @requestActiveDeploy = new RequestActiveDeploy [], { requestId: @options.requestId, deployId: @requestModel.get('activeDeploy').id }
                @requestActiveDeploy.fetched = false
                @requestActiveDeploy.fetch().done =>
                    @requestActiveDeploy.fetched = true
                    @render()

        promises.push @requestHistory.fetch().done =>
            @requestHistory.fetched = true
            @render()

        promises.push @requestDeployHistory.fetch().done =>
            @requestDeployHistory.fetched = true
            @render()

        promises.push @requestTasksActive.fetch().done =>
            @requestTasksActive.fetched = true
            @render()

        promises.push app.collections.tasksScheduled.fetch().done =>
            app.collections.tasksScheduled.fetched = true
            @render()

        $.when(promises...)

    refresh: ->
        @fetch().done =>
            @render()

        @

    render: ->
        context =
            request:
                id: @options.requestId
                name: utils.getRequestNameFromID @options.requestId
                scheduled: false
                onDemand: false
                scheduledOrOnDemand: false
                fullObject: false

            fetchDoneRequestActiveDeploy: @requestActiveDeploy.fetched
            requestActiveDeploy: @requestActiveDeploy.attributes

            requestNameStringLengthTens: Math.floor(@options.requestId.length / 10) * 10

            fetchDoneHistory: @requestHistory.fetched
            requestHistory: @requestHistory.attributes

            fetchDoneDeployHistory: @requestDeployHistory.fetched
            requestDeployHistory: @requestDeployHistory.attributes

            fetchDoneActive: @requestTasksActive.fetched
            requestTasksActive: _.pluck(@requestTasksActive.models, 'attributes')

            fetchDoneScheduled: app.collections.tasksScheduled.fetched
            requestTasksScheduled: _.filter(_.pluck(app.collections.tasksScheduled.models, 'attributes'), (t) => t.requestId is @options.requestId)

        _.extend context.request, @requestModel.attributes

        if @requestHistory.attributes.requestUpdates?.length
            requestLikeObject = $.extend {}, @requestHistory.attributes.requestUpdates[0].request
            delete requestLikeObject.JSONString
            delete requestLikeObject.localRequestHistoryId

            if @requestHistory.attributes.requestUpdates[0].state is 'PAUSED'
                context.request.paused = true

            requestLikeObject.JSONString = utils.stringJSON requestLikeObject
            app.allRequests[requestLikeObject.id] = requestLikeObject
            context.request.fullObject = true

            context.request.scheduled = utils.isScheduledRequest requestLikeObject
            context.request.onDemand = utils.isOnDemandRequest requestLikeObject
            context.request.scheduledOrOnDemand = context.request.scheduled or context.request.onDemand

        $requestHeader = @$el.find('[data-request-header]')
        $requestTasksActiveTableContainer = @$el.find('[data-request-tasks-active-table-container]')
        $requestTasksScheduledTableContainer = @$el.find('[data-request-tasks-scheduled-table-container]')
        $requestHistory = @$el.find('[data-request-history]')
        $requestActiveDeploy = @$el.find('[data-request-active-deploy]')
        $requestInfo = @$el.find('[data-request-info]')
        $requestDeployHistory = @$el.find('[data-request-deploy-history]')

        partials =
            partials:
                requestHeader: @requestHeaderTemplate
                requestTasksActiveTable: @requestTasksActiveTableTemplate
                requestTasksScheduledTable: @requestTasksScheduledTableTemplate
                requestHistory: @requestHistoryTemplate
                requestActiveDeploy: @requestActiveDeployTemplate
                requestInfo: @requestInfoTemplate
                requestDeployHistory: @requestDeployHistoryTemplate

        if not $requestTasksActiveTableContainer.length or not $requestTasksScheduledTableContainer.length
            @$el.html @template context, partials
            @renderHistoricalTasksPaginated()
        else
            $requestHeader.html @requestHeaderTemplate context
            $requestTasksActiveTableContainer.html @requestTasksActiveTableTemplate context
            $requestTasksScheduledTableContainer.html @requestTasksScheduledTableTemplate context
            $requestHistory.html @requestHistoryTemplate context
            $requestActiveDeploy.html @requestActiveDeployTemplate context
            $requestInfo.html @requestInfoTemplate context
            $requestDeployHistory.html @requestDeployHistoryTemplate context

        @setupEvents()

        utils.setupSortableTables()

        @

    renderHistoricalTasksPaginated: ->
        requestHistoricalTasksTable = new RequestHistoricalTasksTableView
            requestId: @options.requestId
            count: 10

        @$el.find('.historical-tasks-paginated').html requestHistoricalTasksTable.render().$el

    setupEvents: ->
        @$el.find('[data-action="viewDeployJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'deploy', $(e.target).data('deploy-id')

        @$el.find('[data-action="viewJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'task', $(e.target).data('task-id')

        @$el.find('[data-action="viewObjectJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'request', $(e.target).data('request-id')

        @$el.find('[data-action="viewRequestHistoryJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'requestHistory', $(e.target).data('local-request-history-id')

        @$el.find('[data-action="remove"]').unbind('click').on 'click', (e) =>
            requestModel = new Request id: $(e.target).data('request-id')

            vex.dialog.confirm
                message: @removeRequestTemplate(requestId: requestModel.get('id'))
                buttons: [
                    $.extend({}, vex.dialog.buttons.YES, (text: 'Remove', className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'))
                    vex.dialog.buttons.NO
                ]
                callback: (confirmed) =>
                    return unless confirmed
                    requestModel.destroy()
                    app.router.navigate 'requests', trigger: true

        @$el.find('[data-action="run-request-now"]').unbind('click').on 'click', (e) =>
            requestModel = new Request id: $(e.target).data('request-id')

            requestType = $(e.target).data 'request-type'

            dialogOptions =
                message: "<p>Are you sure you want to run a task for this #{ requestType } request immediately?</p><pre>#{ requestModel.get('id') }</pre>"
                buttons: [
                    $.extend({}, vex.dialog.buttons.YES, text: 'Run now')
                    vex.dialog.buttons.NO
                ]
                callback: (confirmedOrPromptData) =>
                    return if confirmedOrPromptData is false

                    requestModel.run(confirmedOrPromptData).done =>
                        setTimeout =>
                            @refresh()
                        , 3000

            if requestType is 'on-demand'
                dialogType = vex.dialog.prompt
                dialogOptions.message += '<p>Additional command line input (optional):</p>'
            else
                dialogType = vex.dialog.confirm

            dialogType dialogOptions

        @$el.find('[data-action="pause"]').unbind('click').on 'click', (e) =>
            requestModel = new Request id: $(e.target).data('request-id')

            unpause = $(e.target).data('action-unpause') is true

            vex.dialog.confirm
                message: "<p>Are you sure you want to pause this request?</p><pre>#{ requestModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    if unpause
                        requestModel.unpause().done => @refresh()
                    else
                        requestModel.pause().done => @refresh()

        @$el.find('[data-action="run-now"]').unbind('click').on 'click', (e) =>
            taskModel = app.collections.tasksScheduled.get($(e.target).data('task-id'))
            $row = $(e.target).parents('tr')
            $containingTable = $row.parents('table')

            vex.dialog.confirm
                message: "<p>Are you sure you want to run this task immediately?</p><pre>#{ taskModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    taskModel.run()
                    app.collections.tasksScheduled.remove(taskModel)
                    $row.remove()
                    utils.handlePotentiallyEmptyFilteredTable $containingTable, 'task'

module.exports = RequestView
