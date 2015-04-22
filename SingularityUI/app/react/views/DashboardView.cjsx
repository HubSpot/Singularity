DashboardMain = require '../components/dashboard/DashboardMainCmpt'
View = require './BaseView'

# check if we even need to extend this view
class DashboardView extends View

    initialize:  =>
        app.bootstrapReactView @
        @listenTo @collection, 'change', @renderReact
        @refresh()

    ##
    ## Example of a handler. Define method in this view
    ## that modify models/collections, then pass them down
    ## as props to your components
    ##
    ## destroyModel: (id) =>
    ##     model = @collection.get(id)
    ##     model.destroy()

    refresh: =>
        @collection.fetch().done =>
            @renderReact()
      
    # move into collection
    getStarredRequests: ->
        requests = _.pluck @collection.getStarredOnly(), "attributes"
        ## add in sorting later....
        return requests

    renderReact: ->
        totals = @collection.getUserRequestsTotals()
        user = app.user
        username = app.user.get('deployUser')
        
        React.render(
                <DashboardMain
                    totals={totals}
                    addThingHandler={@addThing}
                    starredRequests={@getStarredRequests()}
                    requestsCollection={@collection} 
                    user={user}
                    username={username}
                    refresh={@refresh}
                />, 
                app.pageEl
            )


module.exports = DashboardView