class View extends Backbone.View

  constructor: (@options) ->
    @collections = @options?.collections || {}
    @models = @options?.models || {}
    super
    app.bootstrapReactView @

  # `refresh` can be called by `app` for the global refresh or by the
  # view whenever it requires. It should trigger the necessary fetches
  refresh: ->

module.exports = View