View = require './view'

Request = require '../models/Request'
RequestHistory = require '../models/RequestHistory'

RequestTasks = require '../collections/RequestTasks'
HistoricalTasks = require '../collections/HistoricalTasks'

class RequestView extends View

    template: require './templates/request'

    removeRequestTemplate: require './templates/vex/removeRequest'

    initialize: =>
        @request = app.allRequests[@options.requestId]

        @requestHistory = new RequestHistory {}, requestId: @options.requestId
        @requestHistory.fetch().done =>
            @requestHistory.fetched = true
            @render()

        @requestTasksActive = new RequestTasks [], { requestId: @options.requestId, active: true }
        @requestTasksActive.fetch().done =>
            @requestTasksActive.fetched = true
            @render()

    render: =>
        if not @request
            vex.dialog.alert("<p>Could not open a request by that ID.</p><pre>#{ @options.requestId }</pre>")
            return

        context =
            request: @request

            fetchDoneHistory: @requestHistory.fetched
            requestHistory: @requestHistory.attributes

            fetchDoneActive: @requestTasksActive.fetched
            requestTasksActive: _.pluck(@requestTasksActive.models, 'attributes')

            requestTasksScheduled: _.filter(_.pluck(app.collections.tasksScheduled.models, 'attributes'), (t) => t.requestId is @options.requestId)

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
                        cell: '<td><span title="{{ id }}"><a href="/singularity/task/{{ id }}" data-route="task/{{ id }}">{{#getShortTaskIDMiddleEllipsis name}}{{/getShortTaskIDMiddleEllipsis}}</a></span></td>'
                    ,
                        header: '<th class="sorting" data-sort="lastTaskStatus">Status</th>'
                        cell: '<td>{{ lastStatusHuman }}</td>'
                    ,
                        header: '<th class="sorting" data-sort="createdAt">Created</th>'
                        cell: '<td>{{ createdAtHuman }}</td>'
                    ,
                        header: '<th class="sorting" data-sort="updatedAt">Updated</th>'
                        cell: '<td>{{ updatedAtHuman }}</td>'
                    ,
                        header: '<th>&nbsp;</th>'
                        cell: '''
                            <td class="actions-column">
                                <a data-task-id="{{ id }}" data-action="viewJSON">JSON</a>
                                <a href="/singularity/task/{{ id }}/files/" data-route="/task/{{ id }}/files/">Files</a>
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
            $teebleOuter.html('<center><p>No historical tasks.</p></center>')

    setupEvents: ->
        @$el.find('[data-action="viewJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'task', $(e.target).data('task-id')

        @$el.find('[data-action="viewObjectJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'request', $(e.target).data('request-id')

        @$el.find('[data-action="remove"]').unbind('click').on 'click', (e) =>
            requestModel = new Request id: $(e.target).data('request-id')

            vex.dialog.confirm
                message: @removeRequestTemplate(requestId: requestModel.get('id'))
                callback: (confirmed) =>
                    return unless confirmed
                    requestModel.destroy()
                    app.router.navigate 'requests', trigger: true

        $runNowLinks = @$el.find('[data-action="run-now"]')

        $runNowLinks.unbind('click').on 'click', (e) =>
            taskModel = app.collections.tasksScheduled.get($(e.target).data('task-id'))
            $row = $(e.target).parents('tr')

            vex.dialog.confirm
                message: "<p>Are you sure you want to run this task immediately:</p><pre>#{ taskModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    taskModel.run()
                    app.collections.tasksScheduled.remove(taskModel)
                    $row.remove()

module.exports = RequestView