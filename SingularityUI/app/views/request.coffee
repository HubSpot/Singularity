View = require './view'

RequestTasks = require '../collections/RequestTasks'

class RequestView extends View

    template: require './templates/request'

    initialize: =>
        @request = app.allRequests[@options.requestId]

        count = 0
        @requestTasksActive = new RequestTasks [], { requestId: @options.requestId, active: true }
        @requestTasksActive.fetch().done =>
            @fetchDoneActive = true
            if ++count is 2
              @render()

        @requestTasksHistorical = new RequestTasks [], { requestId: @options.requestId, active: false }
        @requestTasksHistorical.fetch().done =>
            @fetchDoneHistorical = true
            if ++count is 2
              @render()

    render: =>
        if not @request
            vex.dialog.alert("<p>Could not open a request by that ID.</p><pre>#{ @options.requestId }</pre>")
            return

        context =
            request: @request

            fetchDoneActive: @fetchDoneActive
            fetchDoneHistorical: @fetchDoneHistorical

            requestTasksActive: _.pluck(@requestTasksActive.models, 'attributes')
            requestTasksHistorical: _.filter(_.pluck(@requestTasksHistorical.models, 'attributes'), (t) => not t.isActive)

            requestTasksScheduled: _.filter(_.pluck(app.collections.tasksScheduled.models, 'attributes'), (t) => t.requestId is @options.requestId)

        @$el.html @template context

        @renderHistoricalTasksPaginated()

        @setupEvents()

        utils.setupSortableTables()

    renderHistoricalTasksPaginated: ->
        if @historicalTasksPaginated
            $('.historical-tasks-paginated').html @historicalTasksPaginated.render().el
            return

        @teebleView = Teeble.TableView.extend({})

        @PaginatedCollection = Teeble.ClientCollection.extend(
            model: Backbone.Model
            paginator_core:
                url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @options.requestId }/tasks"
                type: "GET"
                dataType: "json"

            paginator_ui:
                firstPage: 1
                currentPage: 1
                perPage: 5
                totalPages: 50
                pagesInRange: 10

            server_api:
                count: -> @perPage
                page: -> @currentPage
                orderBy: 'updatedAt'

            parse: (tasks) ->
                # @totalPages = 50#Math.ceil(tasks / this.perPage)
                # @totalRecords = 50#parseInt(tasks, 10)

                # TODO abstract
                # since duplicated from RequestTasks.coffee
                _.each tasks, (task) ->
                    task.id = task.taskId.id
                    task.name = task.id
                    task.updatedAtHuman = if task.updatedAt? then moment(task.updatedAt).from() else ''
                    task.createdAtHuman = if task.createdAt? then moment(task.createdAt).from() else ''
                    task.lastStatusHuman = if constants.taskStates[task.lastStatus] then constants.taskStates[task.lastStatus].label else ''
                    task.isActive = if constants.taskStates[task.lastStatus] then constants.taskStates[task.lastStatus].isActive else false

                tasks
        )

        @paginatedItems = new @PaginatedCollection()
        @paginatedItems.fetch
            reset: true
            success: =>
                @paginatedItems.pager()
                @historicalTasksPaginated = new @teebleView(
                    compile: Shlandlebars.compile # oh yeah...
                    collection: @paginatedItems
                    pagination: true
                    table_class: 'table'
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
                )

                @historicalTasksPaginated.setElement $('.historical-tasks-paginated')[0]
                @historicalTasksPaginated.render()

                @setupEvents()

    setupEvents: ->
        @$el.find('[data-action="viewJSON"]').unbind('click').click (event) ->
            utils.viewJSON 'task', $(event.target).data('task-id')

        @$el.find('[data-action="viewObjectJSON"]').unbind('click').click (event) ->
            utils.viewJSON 'request', $(event.target).data('request-id')

module.exports = RequestView
