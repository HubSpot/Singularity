# Base controller to be extended by other classes
class Controller
    
    # Reference to the primary view being used
    view = undefined

    # Keep track of models and collections
    models      = {}
    collections = {}

    # Initialize should bootstrap models/controllers and views
    initialize: ->

    # `refresh` can be called by `app` for the global refresh or by the
    # view whenever it requires. It should trigger the necessary fetchhes
    refresh: ->


module.exports = Controller
