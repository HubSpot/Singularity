View = require './view'

RequestHistoricalTasksTableView = require './requestHistoricalTasksTable'
RequestDeployHistoryTableView = require './requestDeployHistoryTable'
RequestHistoryTableView = require './requestHistoryTable'

Request = require '../models/Request'
RequestActiveDeploy = require '../models/RequestActiveDeploy'

RequestTasks = require '../collections/RequestTasks'

class RequestView extends View

    template: require './templates/request'

    requestHeaderTemplate: require './templates/requestHeader'
    requestTasksActiveTableTemplate: require './templates/requestTasksActiveTable'
    requestTasksScheduledTableTemplate: require './templates/requestTasksScheduledTable'
    requestActiveDeployTemplate: require './templates/requestActiveDeploy'
    requestInfoTemplate: require './templates/requestInfo'

    removeRequestTemplate: require './templates/vex/removeRequest'

    firstRender: true

    initialize: ->
        @requestModel = new Request id: @options.requestId
        @requestModel.fetched = false

        @requestTasksActive = new RequestTasks [], { requestId: @options.requestId, active: true }
        @requestTasksActive.fetched = false

        @requestActiveDeploy = { attributes: {}, mock: true }

    fetch: ->
        # Note some other fetching is deferred until the request history subview/table is fetched

        @requestTasksActive.fetch().done =>
            @requestTasksActive.fetched = true
            @render()

        app.collections.tasksScheduled.fetch().done =>
            app.collections.tasksScheduled.fetched = true
            @render()
        
        app.collections.requestsPending.fetch().done =>
            if (app.collections.requestsPending.get @requestModel.get "id")?
                @$el.find("#pending-alert").removeClass "hide"
        
        app.collections.requestsCleaning.fetch().done =>
            if (app.collections.requestsCleaning.get @requestModel.get "id")?
                @$el.find("#cleaning-alert").removeClass "hide"

    refresh: ->
        @refreshCount ?= 0
        @refreshCount += 1

        # Will automatically kick off several renders (yuck)
        @fetch()

        # Since the parent view is calling refresh immediately, don't refresh
        # all subviews on the first fetch (ghetto way to prevent unnecessary HTTP
        # calls from too tightly coupled views)
        if @refreshCount > 1
            @requestHistoricalTasksTable?.refresh()
            @requestDeployHistoryTable?.refresh()
            @requestHistoryTable?.refresh()

        @

    render: ->
        context = @gatherContext()

        if @firstRender
            @$el.html @template context, @gatherPartials()
            @saveSelectors()
            @firstRender = false
        else
            @$requestHeader.html @requestHeaderTemplate context
            @$requestTasksActiveTableContainer.html @requestTasksActiveTableTemplate context
            @$requestTasksScheduledTableContainer.html @requestTasksScheduledTableTemplate context
            @$requestActiveDeploy.html @requestActiveDeployTemplate context
            @$requestInfo.html @requestInfoTemplate context

        @renderHistoricalTasksPaginatedIfNeeded()
        @renderDeployHistoryPaginatedIfNeeded()
        @renderHistoryPaginatedIfNeeded()

        @setupEvents()

        @$el.find('pre').each -> utils.setupCopyPre $ @

        @

    gatherContext: ->
        context =
            request:
                id: @options.requestId
                name: utils.getRequestNameFromID @options.requestId
                scheduled: false
                onDemand: false
                scheduledOrOnDemand: false
                fullObject: false

            fetchDoneRequestActiveDeploy: @requestActiveDeploy.fetched
            noDataRequestActiveDeploy: @requestActiveDeploy.noData
            requestActiveDeploy: @requestActiveDeploy.attributes

            requestNameStringLengthTens: Math.floor(@options.requestId.length / 10) * 10

            fetchDoneActive: @requestTasksActive.fetched
            requestTasksActive: _.pluck(@requestTasksActive.models, 'attributes')

            fetchDoneScheduled: app.collections.tasksScheduled.fetched
            requestTasksScheduled: _.filter(_.pluck(app.collections.tasksScheduled.models, 'attributes'), (t) => t.requestId is @options.requestId)

        _.extend context.request, @requestModel.attributes


        # Reaching into a subview to pick out a model (just to get things done) :/
        if @requestHistoryTable?.hasHistoryItems()
            firstHistoryItem = @requestHistoryTable.firstItem()
            requestLikeObject = $.extend {}, firstHistoryItem.get 'request'
            delete requestLikeObject.JSONString
            delete requestLikeObject.localRequestHistoryId

            if firstHistoryItem.get('state') is 'PAUSED'
                context.request.paused = true

            if firstHistoryItem.get('state') is 'DELETED'
                context.request.deleted = true

            requestLikeObject.JSONString = utils.stringJSON requestLikeObject
            app.allRequests[requestLikeObject.id] = requestLikeObject
            context.request.fullObject = true

            context.request.scheduled = utils.isScheduledRequest requestLikeObject
            context.request.onDemand = utils.isOnDemandRequest requestLikeObject
            context.request.scheduledOrOnDemand = context.request.scheduled or context.request.onDemand

            context.firstRequestHistoryItem = firstHistoryItem.attributes

        context


    gatherPartials: ->
        partials =
            partials:
                requestHeader: @requestHeaderTemplate
                requestTasksActiveTable: @requestTasksActiveTableTemplate
                requestTasksScheduledTable: @requestTasksScheduledTableTemplate
                requestActiveDeploy: @requestActiveDeployTemplate
                requestInfo: @requestInfoTemplate

    saveSelectors: ->
        @$requestHeader = @$el.find('[data-request-header]')
        @$requestTasksActiveTableContainer = @$el.find('[data-request-tasks-active-table-container]')
        @$requestTasksScheduledTableContainer = @$el.find('[data-request-tasks-scheduled-table-container]')
        @$requestActiveDeploy = @$el.find('[data-request-active-deploy]')
        @$requestInfo = @$el.find('[data-request-info]')
        @$requestDeployHistory = @$el.find('[data-request-deploy-history]')


    renderHistoricalTasksPaginatedIfNeeded: ->
        return if @requestHistoricalTasksTable?

        @requestHistoricalTasksTable = new RequestHistoricalTasksTableView
            requestId: @options.requestId
            count: 10

        @$el.find('.historical-tasks-paginated').html @requestHistoricalTasksTable.render().$el

    renderDeployHistoryPaginatedIfNeeded: ->
        return if @requestDeployHistoryTable?

        @requestDeployHistoryTable = new RequestDeployHistoryTableView
            requestId: @options.requestId
            count: 10

        @$el.find('.deploy-history-paginated').html @requestDeployHistoryTable.render().$el

    renderHistoryPaginatedIfNeeded: ->
        return if @requestHistoryTable?

        @requestHistoryTable = new RequestHistoryTableView
            requestId: @options.requestId
            count: 10

        @$el.find('.history-paginated').html @requestHistoryTable.render().$el

        # More fetching after we know the latest request state (from the first item of the history)
        @listenTo @requestHistoryTable.history, 'sync', =>
            if @requestHistoryTable.hasHistoryItems() and not @requestHistoryTable.isPausedOrDeleted()
                @requestModel.fetch({ suppressErrors: true }).fail =>
                    @requestModel.fetched = true
                    @requestActiveDeploy.fetched = true
                    @requestActiveDeploy.noData = true
                    @render()
                .done =>
                    @requestModel.fetched = true
                    @render()

                    if @requestModel.get('activeDeploy')?
                        if @requestActiveDeploy.mock
                            @requestActiveDeploy = new RequestActiveDeploy [], { requestId: @options.requestId, deployId: @requestModel.get('activeDeploy').id }
                        @requestActiveDeploy.fetch().done =>
                            @requestActiveDeploy.fetched = true
                            @render()
                    else
                        @requestActiveDeploy.fetched = true
                        @requestActiveDeploy.noData = true
                        @render()
            else
                @requestModel.fetched = true
                @requestActiveDeploy.fetched = true
                @requestActiveDeploy.noData = true


    setupEvents: ->

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
            verb = if unpause then 'unpause' else 'pause'

            vex.dialog.confirm
                message: "<p>Are you sure you want to #{verb} this request?</p><pre>#{ requestModel.get('id') }</pre>"
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
