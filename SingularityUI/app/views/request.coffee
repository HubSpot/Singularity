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


    events:
        'click [data-action="viewJSON"]': 'viewJson'
        'click [data-action="viewObjectJSON"]': 'viewObjectJson'
        'click [data-action="viewRequestHistoryJSON"]': 'viewRequestHistoryJson'

        'click [data-action="remove"]': 'removeRequest'
        'click [data-action="run-request-now"]': 'runRequest'
        'click [data-action="pause"]': 'pauseRequest'
        'click [data-action="unpause"]': 'unpauseRequest'
        'click [data-action="bounce"]': 'bounceRequest'

        'click [data-action="run-now"]': 'runTask'

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
            else
                @$el.find("#pending-alert").addClass "hide"
        
        app.collections.requestsCleaning.fetch().done =>
            if (app.collections.requestsCleaning.get @requestModel.get "id")?
                @$el.find("#cleaning-alert").removeClass "hide"
            else
                @$el.find("#cleaning-alert").addClass "hide"

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

        @$el.find('pre').each -> utils.setupCopyPre $ @

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

                    canBeBounced = @requestModel.get('state') in ["ACTIVE", "SYSTEM_COOLDOWN"]
                    canBeBounced = canBeBounced and not @requestModel.get("scheduled")
                    canBeBounced = canBeBounced and not @requestModel.get("onDemand")
                    @requestModel.set "canBeBounced", canBeBounced

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

    viewJson: (e) =>
        utils.viewJSON 'task', $(e.target).data('task-id')

    viewObjectJson: (e) =>
        utils.viewJSON 'request', $(e.target).data('request-id')

    viewRequestHistoryJson: (e) =>
        utils.viewJSON 'requestHistory', $(e.target).data('local-request-history-id')

    removeRequest: (e) =>
        requestModel = new Request id: $(e.target).data('request-id')
        requestModel.promptRemove =>
            app.router.navigate 'requests', trigger: true

    runRequest: (e) =>
        requestModel = new Request id: $(e.target).data('request-id')
        requestModel.promptRun =>
            @refresh()

    pauseRequest: (e) =>
        requestModel = new Request id: $(e.target).data('request-id')
        requestModel.promptPause =>
            @refresh()

    unpauseRequest: (e) =>
        requestModel = new Request id: $(e.target).data('request-id')
        requestModel.promptUnpause =>
            @refresh()
    
    bounceRequest: (e) =>
        requestModel = new Request id: $(e.target).data('request-id')
        requestModel.promptBounce =>
            @refresh()

    runTask: (e) =>
        $row = $(e.target).parents('tr')
        $containingTable = $row.parents('table')
        taskModel = app.collections.tasksScheduled.get($(e.target).data('task-id'))
        
        requestModel = new Request id: taskModel.get "requestId"
        requestModel.promptRun =>
            app.collections.tasksScheduled.remove(taskModel)
            $row.remove()
            utils.handlePotentiallyEmptyFilteredTable $containingTable, 'task'

module.exports = RequestView
