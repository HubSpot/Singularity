Collection = require './collection'
Slave = require '../models/Slave'

class Slaves extends Collection

    model: Slave

    url: => "#{ config.apiRoot }/slaves"

    filterByState: (states) ->
        new Slaves(@filter (model) ->
            model.get('state') in states
        ).toJSON()

    decommissioning_hosts: ->
      dhosts = new Slaves(
        @filter (model) -> model.get('state') in ['DECOMISSIONING','DECOMMISSIONED','DECOMISSIONED','STARTING_DECOMMISSION','STARTING_DECOMISSION']
      )
      .map (model) -> host = model.get('host').split('.')[0].replace(/_/g, "-")

module.exports = Slaves