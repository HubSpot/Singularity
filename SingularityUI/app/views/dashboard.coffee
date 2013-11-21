View = require './view'

class DashboardView extends View

    template: require './templates/dashboard'

    render: =>
        @$el.html @template state: app.state.attributes
        utils.setupSortableTables()

module.exports = DashboardView