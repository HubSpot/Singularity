RequestMain = require '../components/request/RequestMain'
View = require './ReactBaseView'

class RequestView extends View

  synced: false

  initialize: =>
    @renderReact()
    @refresh()

    @models.request.on 'sync', =>
      activeDeploy = @models.request.get 'activeDeploy'
      @renderReact()
      if activeDeploy?.id? and not @models.activeDeployStats.deployId
        @models.activeDeployStats.deployId = activeDeploy.id
        @models.activeDeployStats.fetch().done =>
          @renderReact()

  refresh: ->

    @models.request.fetch()
      .done => @renderReact()
      .error =>
        # ignore 404 so we can still display info about
        # deleted requests (show in `requestHistoryMsg`)
        @ignore404
        app.caughtError()

    if @models.activeDeployStats.deployId?
      @models.activeDeployStats.fetch()
        .done => @renderReact()
        .error @ignore404

    @collections.activeTasks.fetch()
      .done => @renderReact()
      .error    @ignore404

    @collections.scheduledTasks.fetch()
      .done => @renderReact()
      .error @ignore404
    
    if @collections.requestHistory.currentPage is 1
      @collections.requestHistory.fetch()
        .done =>
          @renderReact()
          if @collections.requestHistory.length is 0
            # Request never existed
            app.router.notFound()
        .error => @ignore404

    if @collections.taskHistory.currentPage is 1
      @collections.taskHistory.fetch()
        .done => @renderReact()
        .error    @ignore404
    if @collections.deployHistory.currentPage is 1
      @collections.deployHistory.fetch()
        .done => @renderReact()
        .error  @ignore404

  renderReact: ->
    React.render(
      <RequestMain
        data={@getRenderData()}
        actions={@actions}
      />, app.pageEl
    )

  getRenderData: ->
    synced: @synced
    request: @models.request.toJSON()
    activeDeployStats: @models.activeDeployStats.toJSON()

  actions: =>
    

module.exports = RequestView