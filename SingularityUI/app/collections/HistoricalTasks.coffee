RequestTasks = require './RequestTasks'

class HistoricalTasksCollection extends Mixen(RequestTasks, Teeble.ServerCollection)
    model: Backbone.Model

    paginator_core:
        url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/history/request/#{ @requestId }/tasks"
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
        _.each tasks, (task) ->
            task.JSONString = utils.stringJSON task
            task.id = task.taskId.id
            task.name = task.id
            task.updatedAtHuman = if task.updatedAt? then moment(task.updatedAt).from() else ''
            task.createdAtHuman = if task.createdAt? then moment(task.createdAt).from() else ''
            task.lastStatusHuman = if constants.taskStates[task.lastStatus] then constants.taskStates[task.lastStatus].label else ''
            task.isActive = if constants.taskStates[task.lastStatus] then constants.taskStates[task.lastStatus].isActive else false
            app.allTasks[task.id] = task

        tasks


module.exports = HistoricalTasksCollection
