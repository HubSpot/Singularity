Controller = require './Controller'

Request                = require '../models/Request'
RequestDeployStatus    = require '../models/RequestDeployStatus'

Tasks                  = require '../collections/Tasks'
TaskCleanups           = require '../collections/TaskCleanups'
RequestTasks           = require '../collections/RequestTasks'
HistoricalTasks = require '../collections/HistoricalTasks'
RequestDeployHistory   = require '../collections/RequestDeployHistory'
RequestHistory         = require '../collections/RequestHistory'
Requests               = require '../collections/Requests'

RequestDetailView      = require '../views/request'
PaginatedTableServersideView = require '../views/paginatedTableServersideView'

SimpleSubview          = require '../views/simpleSubview'
RequestHeaderView      = require '../views/requestHeader'
RequestActionExpirationsView = require '../views/requestActionExpirations'

class RequestDetailController extends Controller

    templates:
        requestHistoryMsg:  require '../templates/requestDetail/requestHistoryMsg'
        stats:              require '../templates/requestDetail/requestStats'

        activeTasks:    require '../templates/requestDetail/requestActiveTasks'
        scheduledTasks: require '../templates/requestDetail/requestScheduledTasks'

        # Subview templates
        taskHistory:    require '../templates/requestDetail/requestHistoricalTasks'
        deployHistory:  require '../templates/requestDetail/requestDeployHistory'
        requestHistory: require '../templates/requestDetail/requestHistory'

    initialize: ({@requestId}) ->
        @title @requestId

        #
        # Data stuff
        #
        @models.request = new Request id: @requestId

        @models.activeDeployStats = new RequestDeployStatus
            requestId: @requestId
            deployId:  undefined

        @collections.taskCleanups = new TaskCleanups

        @collections.activeTasks = new RequestTasks [],
            requestId: @requestId
            state:    'active'

        @collections.scheduledTasks = new Tasks [],
            requestId: @requestId
            state:     'scheduled'

        @collections.requestHistory  = new RequestHistory         [], {@requestId}
        @collections.taskHistory     = new HistoricalTasks [], {params: {requestId: @requestId}}
        @collections.deployHistory   = new RequestDeployHistory   [], {@requestId}

        # For starring (never fetched here)
        @collections.requests        = new Requests               [], {}

        #
        # Subviews
        #
        @subviews.header = new RequestHeaderView
            model:          @models.request
            taskCleanups:   @collections.taskCleanups
            activeTasks:    @collections.activeTasks

        @subviews.actionExpirations = new RequestActionExpirationsView
            model:          @models.request

        # would have used header subview for this info,
        # but header expects a request model that
        # no longer exists if a request is deleted
        @subviews.requestHistoryMsg = new SimpleSubview
            collection: @collections.requestHistory
            template:   @templates.requestHistoryMsg
            extraRenderData: (subView) =>
                { request: @models.request.toJSON() }

        @subviews.stats = new SimpleSubview
            model:      @models.activeDeployStats
            template:   @templates.stats

        activeTaskExtraData = -> {}

        if config.displayTaskLabels?.length > 0
            activeTaskExtraData = =>
                unless @models.request.get('activeDeploy')
                    return {}

                requestType = @models.request.get('type')
                deployLabels = @models.request.get('activeDeploy').labels || {}
                taskLabels = @models.request.get('activeDeploy').taskLabels || {}
                {labels, taskLabels} = @models.request.get('activeDeploy')

                applicableLabels = _.filter(config.displayTaskLabels, (label) -> requestType in label.requestTypes)

                unless applicableLabels.length > 0
                    return {}

                data = @collections.activeTasks.map (activeTask) ->
                    combinedLabels = _.extend({}, deployLabels, taskLabels[activeTask.get('taskId').instanceNo] || {})
                    displayLabels = applicableLabels.map (label) ->
                        _.extend({}, label, {labelValue: combinedLabels[label.labelName] || label.labelDefaultValue})
                    _.extend {}, activeTask.toJSON(), {displayLabels}

                return {data, displayTaskLabels: applicableLabels}

        @subviews.activeTasks = new SimpleSubview
            collection: @collections.activeTasks
            template:   @templates.activeTasks
            extraRenderData: activeTaskExtraData

        if config.displayTaskLabels?.length > 0
            @subviews.activeTasks.listenTo @models.request, 'change', @subviews.activeTasks.render

        @subviews.scheduledTasks = new SimpleSubview
            collection:      @collections.scheduledTasks
            template:        @templates.scheduledTasks
            extraRenderData: (subView) =>
                { request: @models.request.toJSON() }

        @subviews.taskHistory = new PaginatedTableServersideView
            collection: @collections.taskHistory
            template:   @templates.taskHistory
            extraRenderData: (subView) =>
                { request: @models.request.toJSON() }

        @subviews.deployHistory = new PaginatedTableServersideView
            collection: @collections.deployHistory
            template:   @templates.deployHistory

        @subviews.requestHistory = new PaginatedTableServersideView
            collection: @collections.requestHistory
            template:   @templates.requestHistory

        #
        # The stats stuff depends on info we get from @models.request
        #
        @models.request.on 'sync', =>
            activeDeploy = @models.request.get 'activeDeploy'
            if activeDeploy?.id? and not @models.activeDeployStats.deployId
                @models.activeDeployStats.deployId = activeDeploy.id
                @models.activeDeployStats.fetch()

        #
        # Main view & stuff
        #
        @setView new RequestDetailView _.extend {@requestId, @subviews},
            model: @models.request
            collection: @collections.requests

        @refresh()

        app.showView @view

    addRequestInfo: =>
        for t in @collections.taskHistory.models
            t.attributes.canBeRunNow = @models.request.attributes.canBeRunNow

    refresh: ->
        requestFetch = @models.request.fetch()

        requestFetch.error =>
            # ignore 404 so we can still display info about
            # deleted requests (show in `requestHistoryMsg`)
            @ignore404
            app.caughtError()

        requestFetch.success =>
          @models.request.set('starred', @collections.requests.isStarred(@models.request.id))

        if @models.activeDeployStats.deployId?
            @models.activeDeployStats.fetch().error @ignore404

        @collections.taskCleanups.fetch().error   @ignore404
        @collections.activeTasks.fetch().error    @ignore404
        @collections.scheduledTasks.fetch().error @ignore404
        @collections.scheduledTasks.fetch({reset: true}).error @ignore404

        if @collections.requestHistory.currentPage is 1
            requestHistoryFetch = @collections.requestHistory.fetch()
            requestHistoryFetch.error => @ignore404
            requestFetch.error =>
                requestHistoryFetch.done =>
                    if @collections.requestHistory.length is 0
                        app.router.notFound()

        requestFetch.done =>
            if @collections.taskHistory.currentPage is 1
                @collections.taskHistory.fetch
                    error:    @ignore404
                    success:  @addRequestInfo
        if @collections.deployHistory.currentPage is 1
            @collections.deployHistory.fetch().error  @ignore404

module.exports = RequestDetailController
