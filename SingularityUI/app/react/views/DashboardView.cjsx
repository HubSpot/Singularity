DashboardMain = require '../components/dashboard/DashboardMainCmpt'
View = require './BaseView'

class DashboardView extends View

    initialize:  =>
        @render()
        # @listenTo @collection, 'sync', @syncUpdate

    render: =>
        console.log 'render main dashboard'        
        React.render(
                <DashboardMain 
                    user=app.user
                    requestsCollection={@collection} 
                />, 
                document.getElementById 'page' 
            )



module.exports = DashboardView