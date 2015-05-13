Collection = require './collection'
Slave = require '../models/Slave'

class Slaves extends Collection

    model: Slave

    url: => "#{ config.apiRoot }/slaves"

    filterByState: (states) ->
        new Slaves(@filter (model) ->
            model.get('state') in states
        ).toJSON()

module.exports = Slaves
