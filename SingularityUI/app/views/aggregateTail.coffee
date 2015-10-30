View = require './view'

AggregateTail = require '../components/aggregateTail/AggregateTail'

class AggregateTailView extends View

    events: ->

    initialize: ({@requestId, @path, @ajaxError, @offset}) ->


    render: =>
      Factory = React.createFactory(AggregateTail)
      root = Factory({ text: 'prop' })
      React.render(root, @el);

module.exports = AggregateTailView
