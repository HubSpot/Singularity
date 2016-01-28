View = require './view'

TaskSearch = require '../components/taskSearch/TaskSearch'

class TaskSearchView extends View

	initialize: (@requestId, opts) ->
		#

	handleViewChange: =>

	render: ->
      $(@el).addClass("task-search-root")
      ReactDOM.render(
        <TaskSearch />,
        @el);


module.exports = TaskSearchView