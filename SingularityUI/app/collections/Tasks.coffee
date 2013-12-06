Collection = require './collection'

class Tasks extends Collection

    comparator: (a, b) ->
        a = a.get('name').toLowerCase()
        b = b.get('name').toLowerCase()
        return 0 if a is b
        return -1 if a < b
        1

module.exports = Tasks