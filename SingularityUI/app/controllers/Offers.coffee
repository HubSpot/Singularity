Controller = require './Controller'

OfferResults = require '../collections/OfferResults'

OffersView = require '../views/offers'

class OffersController extends Controller

    initialize: () ->
        app.showPageLoader()
        @title 'Offers'
        @collections.offers = new OfferResults []
        @setView new OffersView
            collection: @collections.offers

        app.showView @view

        @refresh()

    refresh: ->
        @collections.offers.fetch()

module.exports = OffersController