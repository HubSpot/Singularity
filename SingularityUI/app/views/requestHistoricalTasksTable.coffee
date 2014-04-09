View = require './view'

HistoricalTasks = require '../collections/HistoricalTasks'

class RequestHistoricalTasksTableView extends View

    render: ->
        @$el.html '<div class="page-loader centered cushy"></div>'

        @historicalTasks = new HistoricalTasks [],
            requestId: @options.requestId
            active: false
            sortColumn: 'updatedAt'
            sortDirection: 'asc'

        $.extend @historicalTasks,
            totalPages: 100
            totalRecords: 10000
            currentPage: 1
            firstPage: 1
            perPage: @options.count

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
                        cell: '<td class="visible-desktop">{{ lastTaskStateHuman }}</td>'
                    ,
                        header: '<th class="sorting visible-desktop" data-sort="lastTaskStatus">Deploy ID</th>'
                        cell: '<td class="visible-desktop">{{ deployId }}</td>'
                    ,
                        header: '<th class="sorting visible-desktop" data-sort="startedAt">Started</th>'
                        cell: '<td class="visible-desktop">{{ startedAtHuman }}</td>'
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

                @historicalTasksView.setElement @el

                @historicalTasksView.render()
                @postHistoricalTasksViewRender()

                @historicalTasks.on 'sync', =>
                    @historicalTasksView.render()
                    @postHistoricalTasksViewRender()

        @

    postHistoricalTasksViewRender: ->
        @fixPagination()
        @setupEvents()

    fixPagination: ->
        $empty = @$el.find('.teeble_empty')
        if $empty.length
            if not @tableEverNotEmpty
                @$el.html('<div class="empty-table-message"><p>No historical tasks</p></div>')
            else
                $empty.html '''<center><p>Unfortunately, the API didn't tell us how many total historical tasks there were, so we had let you go one page too far. Please page back now. (Ask @wsorenson...) :)</p></center>'''
        else
            if @$el.find('tbody tr').length < @options.count
                if @$el.find('.pagination-previous').hasClass('disabled')
                    @$el.find('.pagination').hide()
                else
                    @$el.find('.pagination-next').addClass('disabled')
            @tableEverNotEmpty = true

    setupEvents: ->
        @$el.find('[data-action="viewJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'task', $(e.target).data('task-id')

module.exports = RequestHistoricalTasksTableView