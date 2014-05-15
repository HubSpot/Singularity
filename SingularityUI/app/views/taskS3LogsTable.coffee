View = require './view'

class TaskS3LogsTableView extends View

    initialize: ->
        super
        @options.count ?= 10

    render: ->
        @$el.html '<div class="page-loader centered cushy"></div>'


        class TaskS3LogsPaginationView extends Teeble.PaginationView
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

        @collection.pager
            reset: true
            success: =>
                @tableView = new Teeble.TableView
                    compile: Handlebars.compile
                    collection: @collection
                    pagination: true
                    table_class: 'table teeble-table'
                    subviews: $.extend {}, @subviews,
                        pagination: TaskS3LogsPaginationView
                    partials: [
                        header: '<th class="half-table">Log file</th>'
                        cell: '<td><a class="long-link" href="{{ url }}" target="_blank">{{ key }}</a></td>'
                    ,
                        header: '<th>Size</th>'
                        cell: '<td>{{ sizeHuman }}</td>'
                    ,
                        header: '<th>Last modified</th>'
                        cell: '<td data-value="{{ lastModified }}">{{ lastModifiedHuman }}</td>'
                    ,
                        header: '<th></th>'
                        cell: """<td class="actions-column">
                                   <a href="{{ url }}" target="_blank">Download</a>
                                 </td>"""
                    ]

                @tableView.setElement @el

                @tableView.render()
                @postTaskS3LogsTableViewRender()

                @collection.on 'sync', =>
                    @tableView.render()
                    @postTaskS3LogsTableViewRender()

        @

    postTaskS3LogsTableViewRender: ->
        @fixPagination()

    fixPagination: ->
        $empty = @$el.find('.teeble_empty')
        if $empty.length
            if not @tableEverNotEmpty
                @$el.html('<div class="empty-table-message"><p>No logs</p></div>')
            else
                $empty.html '''<center><p>Unfortunately, the API didn't tell us how many total logs there are, so we let you go one page too far. Please page back now. (Ask @wsorenson...) :)</p></center>'''
        else
            if @$el.find('tbody tr').length < @options.count
                if @$el.find('.pagination-previous').hasClass('disabled')
                    @$el.find('.pagination').hide()
                else
                    @$el.find('.pagination-next').addClass('disabled')
            @tableEverNotEmpty = true

module.exports = TaskS3LogsTableView
