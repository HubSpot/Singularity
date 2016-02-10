View = require './view'

TaskSearch = require '../components/taskSearch/TaskSearch'

class TaskSearchView extends View

    initialize: ({@requestId, @global}, opts) ->

    render: ->
      $(@el).addClass("task-search-root")
      ReactDOM.render(
        <TaskSearch 
         initialRequestId = {@requestId}
         global = {@global}
        />,
        @el);


module.exports = TaskSearchView