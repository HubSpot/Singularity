View = require './view'

RequestTasks = require '../collections/RequestTasks'

HistoricalTasksCollection = require '../collections/HistoricalTasks'

class RequestView extends View

    template: require './templates/request'

    initialize: =>
        @request = app.allRequests[@options.requestId]

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

            fetchDoneActive: @requestTasksActive.fetched
            requestTasksActive: _.pluck(@requestTasksActive.models, 'attributes')

            requestTasksScheduled: _.filter(_.pluck(app.collections.tasksScheduled.models, 'attributes'), (t) => t.requestId is @options.requestId)

        @$el.html @template context

        @renderHistoricalTasksPaginated()

        @setupEvents()

        utils.setupSortableTables()

    renderHistoricalTasksPaginated: ->
        @historicalTasks = new HistoricalTasksCollection [], { requestId: @options.requestId, active: false }

        $.extend @historicalTasks,
            totalPages: 100
            totalRecords: 1000
            currentPage: 1
            firstPage: 1
            perPage: 10

        class HistoryPaginationView extends Teeble.PaginationView
            template: '''
                <div class=" <%= pagination_class %>">
                    <ul>
                        <li>
                            <a href="#" class="pagination-previous previous <% if (prev_disabled){ %><%= pagination_disabled %><% } %>">
                                <span class="left"></span>
                                Newer
                            </a>
                        </li>
                        <% _.each(pages, function(page) { %>
                            <li>
                                <a href="#" class="pagination-page <% if (page.active){ %><%= pagination_active %><% } %>" data-page="<%= page.number %>"><%= page.number %></a>
                            </li>
                        <% }); %>
                        <li>
                            <a href="#" class="pagination-next next <% if(next_disabled){ %><%= pagination_disabled %><% } %>">
                                Older
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
                    table_class: 'table'
                    subviews: $.extend {}, @subviews,
                        pagination: HistoryPaginationView
                    partials: [
                        header: '<th class="sorting" data-sort="name">Name</th>'
                        cell: '<td><span title="{{ id }}"><a href="/singularity/task/{{ id }}" data-route="task/{{ id }}">{{#hardBreak name}}{{/hardBreak}}</a></span></td>'
                    ,
                        header: '<th class="sorting" data-sort="lastStatus">Status</th>'
                        cell: '<td>{{ lastStatusHuman }}</td>'
                    ,
                        header: '<th class="sorting" data-sort="createdAt">Created</th>'
                        cell: '<td>{{ createdAtHuman }}</td>'
                    ,
                        header: '<th class="sorting" data-sort="updatedAt">Updated</th>'
                        cell: '<td>{{ updatedAtHuman }}</td>'
                    ,
                        header: '<th class="sorting" data-sort="id">&nbsp;</th>'
                        cell: '''
                            <td>
                                <a data-task-id="{{ id }}" data-action="viewJSON">JSON</a>
                                &nbsp;&nbsp;
                                <a href="/singularity/task/{{ id }}/files/" data-route="/task/{{ id }}/files/">Files</a>
                            </td>
                        '''
                    ]

                @historicalTasksView.setElement $('.historical-tasks-paginated')[0]
                @historicalTasksView.render()

                @historicalTasks.on 'sync', => @historicalTasksView.render()

                @setupEvents()

    setupEvents: ->
        @$el.find('[data-action="viewJSON"]').unbind('click').click (event) ->
            utils.viewJSON 'task', $(event.target).data('task-id')

        @$el.find('[data-action="viewObjectJSON"]').unbind('click').click (event) ->
            utils.viewJSON 'request', $(event.target).data('request-id')

module.exports = RequestView