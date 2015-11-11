Collection = require './collection'

OfferResult = require '../models/OfferResult'

class OfferResults extends Collection

    model: OfferResult

    url: => "#{ config.apiRoot }/state/offers"

    initialize: (models) =>

module.exports = OfferResults
