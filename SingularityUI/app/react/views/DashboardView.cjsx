DashboardMain = require '../components/dashboard/DashboardMainCmpt'
View = require './BaseView'

class DashboardView extends View

    # Set this view as app.currentView so app.globalRefresh
    # can trigger this view to refresh().
    # Listen to events and kick off the initial fetch.
    initialize:  =>        
        @listenTo @collection, 'change', @renderReact
        @listenTo app.user, 'change', @renderReact
    
        @refresh()

    ## Fetch data and then pass it to components as
    ## as plain objects. This keeps backbone's business
    ## out of react's business.
    refresh: =>
        @collection.fetch().done =>
            @renderReact()
      
    # (simplified for this example, e.g. logic should live in
    # collection and it doesnt sort)
    getStarredRequests: ->
        requests = _.pluck @collection.getStarredOnly(), "attributes"
        return requests

    ## Pass data into the parent component as plain objects 
    renderReact: ->
        totals = @collection.getUserRequestsTotals()
        user = app.user
        username = app.user.get('deployUser')
        
        React.render(
                <DashboardMain
                    totals={totals}
                    starredRequests={@getStarredRequests()}
                    requestsCollection={@collection} 
                    user={user}
                    username={username}
                    refresh={@refresh}
                />, 
                app.pageEl
            )

    ##
    ## Example of a handler that modifies models/collection
    ##
    ## Define these methods in this view, then pass 
    ## them down as props to your components. This keeps
    ## backbone's business out react's business. 
    ##
    ## destroyModel: (id) =>
    ##     model = @collection.get(id)
    ##     model.destroy()


module.exports = DashboardView