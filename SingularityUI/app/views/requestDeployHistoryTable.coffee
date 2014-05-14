View = require './view'

DeployHistory = require '../collections/DeployHistory'

class RequestDeployHistoryTableView extends View

    render: ->
        @$el.html '<div class="page-loader centered cushy"></div>'

        @deployHistory = new DeployHistory [],
            requestId: @options.requestId

        $.extend @deployHistory,
            totalPages: 100
            totalRecords: 10000
            currentPage: 1
            firstPage: 1
            perPage: @options.count

        class DeployHistoryPaginationView extends Teeble.PaginationView
            template: '''
                <div class="<%= pagination_class %>">
                    <ul>
                        <li>
                            <a href="#" class="pagination-previous previous <% if (prev_disabled){ %><%= pagination_disabled %><% } %>">
                                <span class="left"></span>
                                Previous
                            </a>
                        </li>
                        <!-- Hide the actual page links until we have an API that returns the count
                        <% _.each(pages, function(page) { %>
                            <li>
                                <a href="#" class="pagination-page <% if (page.active){ %><%= pagination_active %><% } %>" data-page="<%= page.number %>"><%= page.number %></a>
                            </li>
                        <% }); %>
                        -->
                        <li>
                            <a href="#" class="pagination-next next <% if(next_disabled){ %><%= pagination_disabled %><% } %>">
                                Next
                                <span class="right"></span>
                            </a>
                        </li>
                    </ul>
                </div>
            '''

        @deployHistory.pager
            reset: true
            success: =>
                @deployHistoryView = new Teeble.TableView
                    compile: Handlebars.compile
                    collection: @deployHistory
                    pagination: true
                    table_class: 'table teeble-table'
                    subviews: $.extend {}, @subviews,
                        pagination: DeployHistoryPaginationView
                    partials: [
                        header: '<th>Deploy ID</th>'
                        cell: '<td>{{ deployId }}</td>'
                    ,
                        header: '<th>Status</th>'
                        cell: '<td>{{ deployResult.deployStateHuman }}</td>'
                    ,
                        header: '<th>User</th>'
                        cell: '<td>{{ user }}</td>'
                    ,
                        header: '<th>Timestamp</th>'
                        cell: '<td data-value="{{ timestamp }}">{{ timestampHuman }}</td>'
                    ,
                        header: '<th></th>'
                        cell: """<td class="actions-column">
                                   <a data-deploy-id="{{ deployId }}" data-action="viewDeployJSON">JSON</a>
                                 </td>"""
                    ]

                @deployHistoryView.setElement @el

                @deployHistoryView.render()
                @postDeployHistoryViewRender()

                @deployHistory.on 'sync', =>
                    @deployHistoryView.render()
                    @postDeployHistoryViewRender()

        @

    postDeployHistoryViewRender: ->
        @fixPagination()

        # Hackily (?) letting the view deploy json action code live in request.coffee for now

    fixPagination: ->
        $empty = @$el.find('.teeble_empty')
        if $empty.length
            if not @tableEverNotEmpty
                @$el.html('<div class="empty-table-message"><p>No deploy history</p></div>')
            else
                $empty.html '''<center><p>Unfortunately, the API didn't tell us how much total deploy history there is, so we had let you go one page too far. Please page back now. (Ask @wsorenson...) :)</p></center>'''
        else
            if @$el.find('tbody tr').length < @options.count
                if @$el.find('.pagination-previous').hasClass('disabled')
                    @$el.find('.pagination').hide()
                else
                    @$el.find('.pagination-next').addClass('disabled')
            @tableEverNotEmpty = true

module.exports = RequestDeployHistoryTableView
