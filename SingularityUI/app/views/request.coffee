View = require './view'

Request = require '../models/Request'
RequestHistory = require '../models/RequestHistory'

RequestTasks = require '../collections/RequestTasks'
HistoricalTasks = require '../collections/HistoricalTasks'

class RequestView extends View

    template: require './templates/request'

    removeRequestTemplate: require './templates/vex/removeRequest'

    initialize: ->
        @requestModel = new Request id: @options.requestId
        @requestHistory = new RequestHistory {}, requestId: @options.requestId
        @requestTasksActive = new RequestTasks [], { requestId: @options.requestId, active: true }

    fetch: ->
        promises = []

        @requestModel.fetched = false
        @requestHistory.fetched = false
        @requestTasksActive.fetched = false

        promises.push @requestModel.fetch().done =>
            @requestModel.fetched = true
            @render()

        promises.push @requestHistory.fetch().done =>
            @requestHistory.fetched = true
            @render()

        promises.push @requestTasksActive.fetch().done =>
            @requestTasksActive.fetched = true
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

            fetchDoneHistory: @requestHistory.fetched
            requestHistory: @requestHistory.attributes

            fetchDoneActive: @requestTasksActive.fetched
            requestTasksActive: _.pluck(@requestTasksActive.models, 'attributes')

            requestTasksScheduled: _.filter(_.pluck(app.collections.tasksScheduled.models, 'attributes'), (t) => t.requestId is @options.requestId)

        _.extend context.request, @requestModel.attributes

        if @requestHistory.attributes.requestUpdates?.length
            requestLikeObject = @requestHistory.attributes.requestUpdates[0].request

            requestLikeObject.JSONString = utils.stringJSON requestLikeObject
            app.allRequests[requestLikeObject.id] = requestLikeObject
            context.request.fullObject = true

            context.request.scheduled = utils.isScheduledRequest requestLikeObject
            context.request.onDemand = utils.isOnDemandRequest requestLikeObject
            context.request.scheduledOrOnDemand = context.request.scheduled or context.request.onDemand

        context.requestNameStringLengthTens = Math.floor(context.request.id.length / 10) * 10

        @$el.html @template context

        @renderHistoricalTasksPaginated()

        @setupEvents()

        utils.setupSortableTables()

        @

    renderHistoricalTasksPaginated: ->
        @historicalTasks = new HistoricalTasks [],
            requestId: @options.requestId
            active: false
            sortColumn: 'updatedAt'
            sortDirection: 'asc'

        $.extend @historicalTasks,
            totalPages: 100
            totalRecords: 1000
            currentPage: 1
            firstPage: 1
            perPage: 10

        class HistoryPaginationView extends Teeble.PaginationView
            template: '''
                <div class="<%= pagination_class %>">
                    <ul>
                        <li>
                            <a href="#" class="pagination-previous previous <% if (prev_disabled){ %><%= pagination_disabled %><% } %>">
                                <span class="left"></span>
                                Previous
                            </a>
                        </li>
                        <% _.each(pages, function(page) { %>
                            <li>
                                <a href="#" class="pagination-page <% if (page.active){ %><%= pagination_active %><% } %>" data-page="<%= page.number %>"><%= page.number %></a>
                            </li>
                        <% }); %>
                        <li>
                            <a href="#" class="pagination-next next <% if(next_disabled){ %><%= pagination_disabled %><% } %>">
                                Next
                                <span class="right"></span>
                            </a>
                        </li>
                    </ul>
                </div>
            '''

        @historicalTasks.pager
            reset: true
            success: =>
                @historicalTasksView = new Teeble.TableView
                    compile: Handlebars.compile
                    collection: @historicalTasks
                    pagination: true
                    table_class: 'table teeble-table'
                    subviews: $.extend {}, @subviews,
                        pagination: HistoryPaginationView
                    partials: [
                        header: '<th class="sorting" data-sort="taskId">Name</th>'
                        cell: '<td><span title="{{ id }}"><a href="{{#appRoot}}{{/appRoot}}task/{{ id }}" data-route="task/{{ id }}">{{#getShortTaskIDMiddleEllipsis name}}{{/getShortTaskIDMiddleEllipsis}}</a></span></td>'
                    ,
                        header: '<th class="sorting visible-desktop" data-sort="lastTaskStatus">Status</th>'
                        cell: '<td class="visible-desktop">{{ lastStatusHuman }}</td>'
                    ,
                        header: '<th class="sorting visible-desktop" data-sort="lastTaskStatus">Deploy ID</th>'
                        cell: '<td class="visible-desktop">{{ deployId }}</td>'
                    ,
                        header: '<th class="sorting visible-desktop" data-sort="createdAt">Created</th>'
                        cell: '<td class="visible-desktop">{{ createdAtHuman }}</td>'
                    ,
                        header: '<th class="sorting hidden-phone" data-sort="updatedAt">Updated</th>'
                        cell: '<td class="hidden-phone">{{ updatedAtHuman }}</td>'
                    ,
                        header: '<th class="hidden-phone">&nbsp;</th>'
                        cell: '''
                            <td class="actions-column hidden-phone">
                                <a data-task-id="{{ id }}" data-action="viewJSON">JSON</a>
                                <a href="{{#appRoot}}{{/appRoot}}task/{{ id }}/files/" data-route="/task/{{ id }}/files/">Files</a>
                            </td>
                        '''
                    ]

                @historicalTasksView.setElement $('.historical-tasks-paginated')[0]

                @historicalTasksView.render()
                @postHistoricalTasksViewRender()

                @historicalTasks.on 'sync', =>
                    @historicalTasksView.render()
                    @postHistoricalTasksViewRender()

                @setupEvents()

    postHistoricalTasksViewRender: ->
        $teebleOuter = $(@historicalTasksView.el)
        $empty = $teebleOuter.find('.teeble_empty')
        if $empty.length
            $teebleOuter.html('<div class="empty-table-message"><p>No historical tasks</p></div>')

    setupEvents: ->
        @$el.find('[data-action="viewJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'task', $(e.target).data('task-id')

        @$el.find('[data-action="viewObjectJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'request', $(e.target).data('request-id')

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


        @$el.find('[data-action="run-now"]').unbind('click').on 'click', (e) =>
            taskModel = app.collections.tasksScheduled.get($(e.target).data('task-id'))
            $row = $(e.target).parents('tr')

            vex.dialog.confirm
                message: "<p>Are you sure you want to run this task immediately?</p><pre>#{ taskModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    taskModel.run()
                    app.collections.tasksScheduled.remove(taskModel)
                    $row.remove()

module.exports = RequestView