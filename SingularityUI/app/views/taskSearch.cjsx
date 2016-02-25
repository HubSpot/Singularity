React = require 'react'
View = require './view'

TaskSearch = require '../components/taskSearch/TaskSearch'

class TaskSearchView extends View

    events: ->
        _.extend super,
            'click [data-action="viewJSON"]': 'viewJson'

    viewJson: (e) =>
        $target = $(e.currentTarget).parents 'tr'
        id = $target.data 'id'
        collectionName = $target.data 'collection'

        # Need to reach into subviews to get the necessary data
        collection = @subviews[collectionName].collection
        utils.viewJSON collection.get id

    initialize: ({@requestId, @global}, opts) ->

    render: ->
      $(@el).addClass("task-search-root")
      ReactDOM.render(
        <TaskSearch 
         initialRequestId = {@requestId}
         global = {@global}
         taskSearchViewSuper = super
        />,
        @el);


module.exports = TaskSearchView
