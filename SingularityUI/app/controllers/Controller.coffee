Utils = require '../utils'

# Base controller to be extended by other classes
class Controller

    # Reference to the primary view being used
    view:     undefined
    # Subviews that will be used by the primary view
    subviews: {}

    # Keep track of models and collections
    models:      {}
    collections: {}

    constructor: (params) ->
      # Reset the title between each page
      @title ''
      @initialize?(params)

    # Initialize should bootstrap models/controllers and views
    initialize: ->

    # `refresh` can be called by `app` for the global refresh or by the
    # view whenever it requires. It should trigger the necessary fetches
    refresh: ->

    # Set the primary view and listen to its events
    setView: (@view) ->
        @view.on 'refreshrequest', => @refresh()

    title: (pageTitle) ->
      if pageTitle == ''
        document.title = config.title
      else
        document.title = pageTitle + ' - ' + config.title

    # e.g. `myModel.fetch().error @ignore404`
    ignore404: Utils.ignore404

    # e.g. `myModel.fetch().error @ignore400`
    ignore400: Utils.ignore400

module.exports = Controller
