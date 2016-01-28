View = require './view'

TaskSearch = require '../components/taskSearch/TaskSearch'

class TaskSearchView extends View

	requestId: ''

	initialize: (@requestId, @requestLocked, opts) ->

	handleViewChange: =>

	render: ->
      $(@el).addClass("task-search-root")
      ReactDOM.render(
        <TaskSearch 
         requestId = {@requestId}
         requestLocked = {@requestLocked}
        />,
        @el);


module.exports = TaskSearchView