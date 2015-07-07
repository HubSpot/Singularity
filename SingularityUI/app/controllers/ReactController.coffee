# Base controller to be extended by other classes
class Controller
    
    # Keep track of models and collections
    models:      {}
    collections: {}

    constructor: (params) -> @initialize?(params)
    # Initialize should bootstrap models/controllers and views
    initialize: ->

    # `refresh` can be called by `app` for the global refresh or by the
    # view whenever it requires. It should trigger the necessary fetches
    refresh: ->

    # e.g. `myModel.fetch().error @ignore404`
    ignore404: (response) -> app.caughtError() if response.status is 404

module.exports = Controller
