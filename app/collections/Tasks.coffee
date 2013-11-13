Collection = require './collection'

class Tasks extends Collection

    parseLabelFromName: (name) ->
        name.split(/(\-|\:)/)[0]

    comparator: 'name'

module.exports = Tasks