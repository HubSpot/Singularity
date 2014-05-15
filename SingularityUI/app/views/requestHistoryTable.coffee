View = require './view'

RequestHistory = require '../collections/RequestHistory'

class RequestHistoryTableView extends View

    render: ->
        @$el.html '<div class="page-loader centered cushy"></div>'

        @history = new RequestHistory [],
            requestId: @options.requestId

        $.extend @history,
            totalPages: 100
            totalRecords: 10000
            currentPage: 1
            firstPage: 1
            perPage: @options.count

        class RequestHistoryPaginationView extends Teeble.PaginationView
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

        @history.pager
            reset: true
            success: =>
                @historyView = new Teeble.TableView
                    compile: Handlebars.compile
                    collection: @history
                    pagination: true
                    table_class: 'table teeble-table'
                    subviews: $.extend {}, @subviews,
                        pagination: RequestHistoryPaginationView
                    partials: [
                        header: '<th>State</th>'
                        cell: '<td>{{ stateHuman }}</td>'
                    ,
                        header: '<th>User</th>'
                        cell: '<td>{{ userHuman }}</td>'
                    ,
                        header: '<th>Created</th>'
                        cell: '<td data-value="{{ createdAt }}">{{ createdAtHuman }}</td>'
                    ,
                        header: '<th></th>'
                        cell: """<td class="actions-column">
                                   <a data-local-request-history-id="{{ request.localRequestHistoryId }}" data-action="viewRequestHistoryJSON">JSON</a>
                                 </td>"""
                    ]

                @historyView.setElement @el

                @historyView.render()
                @postHistoryViewRender()

                @history.on 'sync', =>
                    @historyView.render()
                    @postHistoryViewRender()

        @

    refresh: ->
        @history.goTo @history.currentPage

    postHistoryViewRender: ->
        @fixPagination()

        # Hackily (?) letting the view json action code live in request.coffee for now

    fixPagination: ->
        $empty = @$el.find('.teeble_empty')
        if $empty.length
            if not @tableEverNotEmpty
                @$el.html('<div class="empty-table-message"><p>No request history</p></div>')
            else
                $empty.html '''<center><p>Unfortunately, the API didn't tell us how much total request history there is, so we had let you go one page too far. Please page back now. (Ask @wsorenson...) :)</p></center>'''
        else
            if @$el.find('tbody tr').length < @options.count
                if @$el.find('.pagination-previous').hasClass('disabled')
                    @$el.find('.pagination').hide()
                else
                    @$el.find('.pagination-next').addClass('disabled')
            @tableEverNotEmpty = true

module.exports = RequestHistoryTableView
