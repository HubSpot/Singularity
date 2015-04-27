class View extends Backbone.View

  constructor: (params = {}) ->
    super params
    app.bootstrapReactView @

  # `refresh` can be called by `app` for the global refresh or by the
  # view whenever it requires. It should trigger the necessary fetches
  refresh: ->

module.exports = View