Controller = require './Controller'

Request                = require '../models/Request'
RequestActiveDeploy    = require '../models/RequestActiveDeploy'

RequestTasks           = require '../collections/RequestTasks'
RequestHistoricalTasks = require '../collections/RequestHistoricalTasks'
RequestDeployHistory   = require '../collections/RequestDeployHistory'
RequestHistory         = require '../collections/RequestHistory'

RequestDetailView      = require '../views/request'
ExpandableTableSubview = require '../views/expandableTableSubview'
SimpleSubview          = require '../views/simpleSubview'

class RequestDetailController extends Controller

    templates:
        header:         require '../templates/requestDetail/requestHeader'
        stats:          require '../templates/requestDetail/requestStats'

        activeTasks:    require '../templates/requestDetail/requestActiveTasks'
        scheduledTasks: require '../templates/requestDetail/requestScheduledTasks'

        # Subview templates
        taskHistory:    require '../templates/requestDetail/requestHistoricalTasks'
        deployHistory:  require '../templates/requestDetail/requestDeployHistory'
        requestHistory: require '../templates/requestDetail/requestHistory'

    initialize: ({@requestId}) ->
        #
        # Data stuff
        #
        @models.request = new Request id: @requestId

        @models.activeDeployStats = new RequestActiveDeploy
            requestId: @requestId
            deployId:  undefined

        @collections.activeTasks = new RequestTasks [],
            requestId: @requestId
            state: 'active'

        @collections.requestHistory  = new RequestHistory         [], {@requestId}
        @collections.taskHistory     = new RequestHistoricalTasks [], {@requestId}
        @collections.deployHistory   = new RequestDeployHistory   [], {@requestId}

        #
        # Subviews
        #
        @subviews.header = new SimpleSubview
            model:      @models.request
            template:   @templates.header

        @subviews.stats = new SimpleSubview
            model:      @models.activeDeployStats
            template:   @templates.stats

        @subviews.activeTasks = new SimpleSubview
            collection: @collections.activeTasks
            template:   @templates.activeTasks

        @subviews.taskHistory = new ExpandableTableSubview
            collection: @collections.taskHistory
            template:   @templates.taskHistory

        @subviews.deployHistory = new ExpandableTableSubview
            collection: @collections.deployHistory
            template:   @templates.deployHistory

        @subviews.requestHistory = new ExpandableTableSubview
            collection: @collections.requestHistory
            template:   @templates.requestHistory

        #
        # The stats stuff depends on info we get from @models.request
        #
        @models.request.on 'sync', =>
            activeDeploy = @models.request.get 'activeDeploy'
            if activeDeploy?.id?
                @models.activeDeployStats.deployId = activeDeploy.id
                @models.activeDeployStats.fetch()

        #
        # Main view & stuff
        #
        @setView new RequestDetailView _.extend {@requestId, @subviews},
            model: @models.request

        @refresh()

        app.showView @view

    refresh: ->
        @models.request.fetch().error =>
            # Doesn't exist, 404
            app.caughtError()
            app.router.notFound()

        if @models.activeDeployStats.deployId?
            @models.activeDeployStats.fetch().error @ignore404

        @collections.activeTasks.fetch().error    @ignore404
        @collections.requestHistory.fetch().error @ignore404
        @collections.taskHistory.fetch().error    @ignore404
        @collections.deployHistory.fetch().error  @ignore404

module.exports = RequestDetailController
