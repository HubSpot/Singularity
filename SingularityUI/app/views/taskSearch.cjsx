View = require './view'

TaskSearch = require '../components/taskSearch/TaskSearch'

class TaskSearchView extends View

	initialize: ({@requestId, @requestLocked}, opts) ->

	render: ->
      $(@el).addClass("task-search-root")
      ReactDOM.render(
        <TaskSearch 
         initialRequestId = {@requestId}
         requestLocked = {@requestLocked}
        />,
        @el);


module.exports = TaskSearchView