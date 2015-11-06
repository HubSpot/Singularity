View = require './view'

Deploy = require '../models/Deploy'

class DeployView extends View

    template: require '../templates/deployDetail/deployBase'

    events: ->
        _.extend super,
            'click [data-action="viewJSON"]': 'viewJson'
            'click [data-action="viewObjectJSON"]': 'viewObjectJson'

    initialize: ({@requestId, @deployId}) ->

    render: ->
        @$el.html @template
            config: config

        # Attach subview elements
        @$('#header').html              @subviews.header.$el
        @$('#activeTasks').html         @subviews.activeTasks.$el
        @$('#info').html                @subviews.info.$el
        @$('#tasks').html               @subviews.taskHistory.$el
        @$('#healthChecks').html        @subviews.healthChecks.$el

    viewJson: (e) =>
        $target = $(e.currentTarget).parents 'tr'
        id = $target.data 'id'
        collectionName = $target.data 'collection'

        # Need to reach into subviews to get the necessary data
        collection = @subviews[collectionName].collection
        utils.viewJSON collection.get id

    viewObjectJson: (e) =>
        utils.viewJSON @model


module.exports = DeployView
